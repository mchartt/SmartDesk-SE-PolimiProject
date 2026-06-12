package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

/** RESOLVED: stato finale dopo approvazione host. */
@Component
public class ResolvedTicketState implements TicketState {

    /** {@inheritDoc} */
    @Override
    public TicketStatus code() { return TicketStatus.RESOLVED; }

    /** Scrive RESOLVED sul ticket. */
    @Override
    public void enter(Ticket ticket) {
        ticket.setStatus(TicketStatus.RESOLVED.name());
    }

    /**
     * Assegnazione non consentita su ticket già risolto.
     *
     * @throws BusinessRuleException ticket già chiuso positivamente
     */
    @Override
    public void assignTechnician(Ticket ticket, Long technicianId) {
        throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_RESOLVED_APPROVED.text());
    }

    /**
     * Verifica non consentita su ticket già risolto.
     *
     * @throws BusinessRuleException ticket già risolto
     */
    @Override
    public void verify(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_RESOLVED.text());
    }

    /** No-op: già approvato. */
    @Override
    public void approve(Ticket ticket) {
        // Già approvato
    }

    /**
     * Respingimento non consentito su ticket già approvato.
     *
     * @throws BusinessRuleException non si può respingere un ticket approvato
     */
    @Override
    public void reject(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_REJECT_ALREADY_APPROVED.text());
    }

    /**
     * Risoluzione ripetuta non consentita.
     *
     * @throws BusinessRuleException ticket già risolto
     */
    @Override
    public void resolve(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_RESOLVED.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAddComment() { return false; }
}
