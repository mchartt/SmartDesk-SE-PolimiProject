package it.polimi.smartdesk_backend.repository.ticket;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;

/** Storico note worker su ticket, ordinato per {@code createdAt}. */
public interface TicketWorkerNoteRepository extends JpaRepository<TicketWorkerNote, Long> {

    List<TicketWorkerNote> findByTicketIDInOrderByCreatedAtAsc(Collection<Long> ticketIds);

    List<TicketWorkerNote> findByTicketIDOrderByCreatedAtAsc(Long ticketID);
}
