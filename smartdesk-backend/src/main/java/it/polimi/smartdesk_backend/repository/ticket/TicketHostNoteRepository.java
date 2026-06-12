package it.polimi.smartdesk_backend.repository.ticket;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;

/** Note lasciate dall'host sul ticket, ordinate per data. */
public interface TicketHostNoteRepository extends JpaRepository<TicketHostNote, Long> {
    List<TicketHostNote> findByTicketIDOrderByCreatedAtAsc(Long ticketID);
    List<TicketHostNote> findByTicketIDInOrderByCreatedAtAsc(Collection<Long> ticketIDs);
}
