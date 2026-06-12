package it.polimi.smartdesk_backend.repository.ticket;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;

/** Storico note tecnico su ticket, ordinato per {@code createdAt}. */
public interface TicketTechnicianNoteRepository extends JpaRepository<TicketTechnicianNote, Long> {

    List<TicketTechnicianNote> findByTicketIDInOrderByCreatedAtAsc(Collection<Long> ticketIds);

    List<TicketTechnicianNote> findByTicketIDOrderByCreatedAtAsc(Long ticketID);
}
