package it.polimi.smartdesk_backend.service.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import lombok.RequiredArgsConstructor;

/** Pulizia storico prenotazioni worker: rimuove giorni di calendario già trascorsi e recensioni collegate. Criterio {@link #isPastForHistory} allineato alla UI worker: giorno da {@code startTime} se presente, altrimenti {@code bookedDay}; se entrambi assenti, confronto su {@code endTime}. */
@Service
@RequiredArgsConstructor
public class BookingWorkerHistoryService {

    private final BookingRepository bookingRepo;
    private final ReviewRepository reviewRepo;
    private final TicketRepository ticketRepo;
    private final TicketWorkerNoteRepository workerNoteRepo;
    private final TicketTechnicianNoteRepository technicianNoteRepo;
    private final TicketHostNoteRepository hostNoteRepo;

    /**
     * Elimina in transazione tutte le prenotazioni passate del worker e le recensioni collegate (batch).
     *
     * @param workerId ID worker autenticato
     * @return numero di prenotazioni eliminate; {@code 0} se nessuna passata
     */
    @Transactional
    public int clearPastBookingsForWorker(Long workerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<Booking> past = new ArrayList<>();
        for (Booking booking : bookingRepo.findByWorkerID(workerId)) {
            if (isPastForHistory(booking, today, now)) {
                past.add(booking);
            }
        }
        if (past.isEmpty()) {
            return 0;
        }
        List<Long> bookingIds = past.stream().map(Booking::getBookingID).toList();
        var reviewsToDelete = reviewRepo.findByBookingIDIn(bookingIds).stream()
                .filter(r -> workerId.equals(r.getWorkerID()))
                .toList();
        reviewRepo.deleteAll(reviewsToDelete);

        // Pulizia note tecniche correlate ai ticket aperti durante queste prenotazioni
        for (Booking b : past) {
            if (b.getStartTime() == null || b.getEndTime() == null) continue;
            var relatedTickets = ticketRepo.findByDeskID(b.getDeskID()).stream()
                    .filter(t -> workerId.equals(t.getWorkerID()))
                    .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(b.getStartTime()) && !t.getCreatedAt().isAfter(b.getEndTime()))
                    .map(Ticket::getTicketID)
                    .toList();
            
            if (!relatedTickets.isEmpty()) {
                workerNoteRepo.deleteAll(workerNoteRepo.findByTicketIDInOrderByCreatedAtAsc(relatedTickets));
                technicianNoteRepo.deleteAll(technicianNoteRepo.findByTicketIDInOrderByCreatedAtAsc(relatedTickets));
                hostNoteRepo.deleteAll(hostNoteRepo.findByTicketIDInOrderByCreatedAtAsc(relatedTickets));
            }
        }

        bookingRepo.deleteAllById(bookingIds);
        return past.size();
    }

    /** Giorno di calendario della prenotazione (come {@code bookingCalendarDay} lato frontend). */
    private static LocalDate resolveCalendarDay(Booking booking) {
        if (booking.getStartTime() != null) {
            return booking.getStartTime().toLocalDate();
        }
        return booking.getBookedDay();
    }

    /**
     * Verifica se la prenotazione appartiene allo storico passato del worker.
     *
     * @param today giorno corrente (server)
     * @param now istante unico per il confronto su {@code endTime}
     */
    private static boolean isPastForHistory(Booking booking, LocalDate today, LocalDateTime now) {
        LocalDate day = resolveCalendarDay(booking);
        if (day != null) {
            return day.isBefore(today);
        }
        LocalDateTime end = booking.getEndTime();
        return end != null && end.isBefore(now);
    }
}
