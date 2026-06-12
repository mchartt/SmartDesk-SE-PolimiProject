package it.polimi.smartdesk_backend.service.booking;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;

/** Query prenotazioni per worker, host e admin; carica i desk in batch per limitare le query N+1. */
@Service
@RequiredArgsConstructor
public class BookingQueryService {

    private final BookingRepository bookingRepo;
    private final ReviewRepository reviewRepo;
    private final DeskRepository deskRepo;
    private final UserRepository userRepo;
    private final BookingDtoMapper bookingDtoMapper;

    /** Restituisce una prenotazione senza controllo accesso; riservato ad admin e flussi interni. */
    @Transactional(readOnly = true)
    public BookingDTO findById(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingId)));
        return bookingDtoMapper.toDto(booking, loadDesk(booking), null);
    }

    /**
     * Dettaglio con controllo accesso: worker solo sulle proprie, sys admin su tutte.
     * Accesso negato mascherato come 404.
     *
     * @throws NotFoundException prenotazione assente o non visibile al richiedente
     */
    @Transactional(readOnly = true)
    public BookingDTO findByIdForUser(Long bookingId, Long requesterId, Role requesterRole) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingId)));
        if (!canAccessBooking(booking, requesterId, requesterRole)) {
            throw new NotFoundException(ResourceMessage.bookingNotFound(bookingId));
        }
        return bookingDtoMapper.toDto(booking, loadDesk(booking), null);
    }

    private boolean canAccessBooking(Booking booking, Long requesterId, Role requesterRole) {
        if (requesterRole == Role.SYS_ADMIN) {
            return true;
        }
        return requesterId != null && requesterId.equals(booking.getWorkerID());
    }

    /** Elenca tutte le prenotazioni non cancellate di un worker. */
    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByWorker(Long workerID) {
        List<Booking> bookings = bookingRepo.findByWorkerID(workerID).stream()
                .filter(b -> !BookingStatus.CANCELLED.name().equals(b.getStatus()))
                .toList();
        return toDTOs(bookings, false);
    }

    /** Elenco prenotazioni per un host (tutti i desk dei suoi spazi). */
    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByHost(Long hostID) {
        return toDTOs(bookingRepo.findAllByDeskHost(hostID), true);
    }

    /**
     * Vista globale per sys admin, ordinate per {@code startTime} decrescente.
     * Include i dati del worker su ogni riga; senza paginazione (limite didattico del progetto).
     *
     * @return tutte le prenotazioni del sistema
     */
    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookingsForAdmin() {
        return toDTOs(bookingRepo.findAllByOrderByStartTimeDesc(), true);
    }

    /**
     * Prenotazioni concluse e ancora recensibili, senza recensione già inviata.
     * Ordinate per fine slot decrescente.
     *
     * @param workerID worker che può lasciare la recensione
     * @return prenotazioni idonee al flusso review
     */
    @Transactional(readOnly = true)
    public List<BookingDTO> getReviewEligibleBookings(Long workerID) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepo.findByWorkerID(workerID).stream()
                .filter(b -> b.isEligibleForReview(now))
                .filter(b -> !reviewRepo.existsByBookingID(b.getBookingID()))
                .sorted(Comparator.comparing(Booking::getEndTime).reversed())
                .collect(Collectors.collectingAndThen(Collectors.toList(), bookings -> toDTOs(bookings, false)));
    }

    private List<BookingDTO> toDTOs(List<Booking> bookings, boolean includeWorkers) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        Map<Long, Desk> desksById = loadDesks(bookings);
        Map<Long, User> usersById = includeWorkers ? loadUsers(bookings) : Map.of();
        return bookingDtoMapper.toDtoList(bookings, desksById, usersById);
    }

    private Desk loadDesk(Booking booking) {
        return loadDesks(List.of(booking)).get(booking.getDeskID());
    }

    private Map<Long, Desk> loadDesks(List<Booking> bookings) {
        Set<Long> deskIds = bookings.stream()
                .map(Booking::getDeskID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (deskIds.isEmpty()) {
            return Map.of();
        }
        return deskRepo.findAllWithSpaceAndRoomByDeskIdIn(deskIds).stream()
                .collect(Collectors.toMap(Desk::getDeskID, Function.identity()));
    }

    private Map<Long, User> loadUsers(List<Booking> bookings) {
        Set<Long> workerIds = bookings.stream()
                .map(Booking::getWorkerID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (workerIds.isEmpty()) {
            return Map.of();
        }
        return userRepo.findAllById(workerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}

