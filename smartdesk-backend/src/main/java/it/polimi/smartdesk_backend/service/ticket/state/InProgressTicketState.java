package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

/** IN_PROGRESS: lavoro in corso; completamento tecnico → VERIFYING. */
@Component
public class InProgressTicketState implements TicketState {

    /** {@inheritDoc} */
    @Override
    public TicketStatus code() { return TicketStatus.IN_PROGRESS; }

    /** Scrive IN_PROGRESS sul ticket. */
    @Override
    public void enter(Ticket ticket) {
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
    }

    /** Permette di cambiare tecnico senza uscire da IN_PROGRESS. */
    @Override
    public void assignTechnician(Ticket ticket, Long technicianId) {
        // Permettiamo la ri-assegnazione se necessario (es. host cambia tecnico)
        ticket.setTechnicianID(technicianId);
    }

    /** Tecnico manda in verifica host con testo risoluzione. */
    @Override
    public void verify(Ticket ticket, String resolution) {
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.VERIFYING.name());
    }

    /**
     * Approvazione non consentita mentre il ticket è in lavorazione.
     *
     * @throws BusinessRuleException ancora in lavorazione
     */
    @Override
    public void approve(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_IN_PROGRESS_CANNOT_APPROVE.text());
    }

    /**
     * Respingimento non consentito da IN_PROGRESS.
     *
     * @throws BusinessRuleException già in lavorazione
     */
    @Override
    public void reject(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_IN_PROGRESS.text());
    }

    /** Chiusura diretta in RESOLVED (salta la verifica host). */
    @Override
    public void resolve(Ticket ticket, String resolution) {
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.RESOLVED.name());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAddComment() { return true; }
}
