package it.polimi.smartdesk_backend.service.booking;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.BookingMessage;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cancellazione prenotazione worker/host con evento {@code BookingReleasedEvent} e regole su stato booking. L'eliminazione è idempotente: una seconda richiesta concorrente non fallisce. Se il desk non è in manutenzione, pubblica un evento per la waitlist. */
@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Rimuove la prenotazione con lock pessimistico sul desk.
     * Idempotente se un altro thread ha già cancellato; pubblica {@link BookingReleasedEvent} solo se il desk non è in {@code MAINTENANCE}.
     *
     * @param bookingID identificativo della prenotazione
     * @throws NotFoundException prenotazione o desk assenti
     */
    @Transactional
    public void removeBooking(Long bookingID) {
        Booking booking = bookingRepo.findById(bookingID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingID)));
        Long deskId = booking.getDeskID();

        var desk = deskRepo.lockByDeskIdForUpdate(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));

        // Re-fetch to avoid race condition on status check
        booking = bookingRepo.findById(bookingID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingID)));

        if (BookingStatus.CANCELLED.name().equals(booking.getStatus())) {
            // Se la booking è già stata cancellata (o annullata) da un altro thread concorrente.
            // Restituiamo successo per garantire l'idempotenza dell'operazione.
            return;
        }

        booking.cancel();
        bookingRepo.save(booking);

        LocalDateTime freedStart = booking.getStartTime();
        LocalDateTime freedEnd = booking.getEndTime();
        LocalDate bookedDay = booking.getBookedDay();

        if (desk.getStateCode() != DeskStateCode.MAINTENANCE) {
            eventPublisher.publishEvent(new BookingReleasedEvent(deskId, bookedDay, freedStart, freedEnd));
        }
    }

    /**
     * Cancellazione con controllo accesso: il worker solo sulle proprie prenotazioni, il sys admin su tutte.
     * Notifica il worker se l'admin cancella per suo conto; accesso negato mascherato come 404.
     *
     * @param bookingID identificativo prenotazione
     * @param requesterId utente che effettua la richiesta
     * @param requesterRole ruolo del richiedente
     * @throws NotFoundException prenotazione assente o accesso negato
     */
    @Transactional
    public void removeBookingForUser(Long bookingID, Long requesterId, Role requesterRole) {
        Booking booking = bookingRepo.findById(bookingID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingID)));
        if (!canAccessBooking(booking, requesterId, requesterRole)) {
            throw new NotFoundException(ResourceMessage.bookingNotFound(bookingID));
        }
        assertBookingNotStartedForWorkerCancel(booking, requesterRole);
        if (requesterRole == Role.SYS_ADMIN && booking.getWorkerID() != null) {
            notificationService.notifyBookingCancelledByAdmin(booking.getWorkerID(), publicBookingRef(booking));
        }
        removeBooking(bookingID);
    }

    /** Il worker non può annullare una prenotazione già iniziata (deve usare fine sessione). */
    private void assertBookingNotStartedForWorkerCancel(Booking booking, Role requesterRole) {
        if (requesterRole != Role.WORKER) {
            return;
        }
        LocalDateTime start = booking.getStartTime();
        if (start == null) {
            return;
        }
        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!start.isAfter(now)) {
            throw new BusinessRuleException(BookingMessage.BOOKING_CANNOT_CANCEL_IN_PROGRESS.text());
        }
    }

    private boolean canAccessBooking(Booking booking, Long requesterId, Role requesterRole) {
        if (requesterRole == Role.SYS_ADMIN) {
            return true;
        }
        return requesterId != null && requesterId.equals(booking.getWorkerID());
    }

    private static String publicBookingRef(Booking booking) {
        String code = booking.getBookingCode();
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        return String.valueOf(booking.getBookingID());
    }
}

