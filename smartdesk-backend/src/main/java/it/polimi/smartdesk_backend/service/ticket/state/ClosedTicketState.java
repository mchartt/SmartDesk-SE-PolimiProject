package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

/** CLOSED: stato terminale alternativo a RESOLVED (es. desk dismesso). */
@Component
public class ClosedTicketState implements TicketState {

    /** {@inheritDoc} */
    @Override
    public TicketStatus code() { return TicketStatus.CLOSED; }

    /** Scrive CLOSED sul ticket. */
    @Override
    public void enter(Ticket ticket) {
        ticket.setStatus(TicketStatus.CLOSED.name());
    }

    /**
     * Assegnazione non consentita su ticket chiuso.
     *
     * @throws BusinessRuleException ticket chiuso
     */
    @Override
    public void assignTechnician(Ticket ticket, Long technicianId) {
        throw new BusinessRuleException(TicketMessage.TICKET_CLOSED.text());
    }

    /**
     * Verifica non consentita su ticket chiuso.
     *
     * @throws BusinessRuleException ticket chiuso
     */
    @Override
    public void verify(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_CLOSED.text());
    }

    /** No-op: già chiuso. */
    @Override
    public void approve(Ticket ticket) {
        // Già chiuso
    }

    /**
     * Respingimento non consentito su ticket chiuso.
     *
     * @throws BusinessRuleException ticket chiuso
     */
    @Override
    public void reject(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_REJECT_CLOSED.text());
    }

    /**
     * Risoluzione non consentita su ticket chiuso.
     *
     * @throws BusinessRuleException ticket chiuso
     */
    @Override
    public void resolve(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_CLOSED.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAddComment() { return false; }
}
