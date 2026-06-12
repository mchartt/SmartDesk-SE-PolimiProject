package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

/** OPEN: stato iniziale; assegnazione tecnico → IN_PROGRESS. */
@Component
public class OpenTicketState implements TicketState {

    /** {@inheritDoc} */
    @Override
    public TicketStatus code() { return TicketStatus.OPEN; }

    /** Scrive OPEN sul ticket. */
    @Override
    public void enter(Ticket ticket) {
        ticket.setStatus(TicketStatus.OPEN.name());
    }

    /** Assegna il tecnico e passa in lavorazione. */
    @Override
    public void assignTechnician(Ticket ticket, Long technicianId) {
        ticket.assign(technicianId);
    }

    /**
     * Verifica non consentita da OPEN.
     *
     * @throws BusinessRuleException ticket non ancora preso in carico
     */
    @Override
    public void verify(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_VERIFY_NOT_ASSIGNED.text());
    }

    /**
     * Approvazione non consentita da OPEN.
     *
     * @throws BusinessRuleException ticket non ancora risolto
     */
    @Override
    public void approve(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_APPROVE_NOT_RESOLVED.text());
    }

    /**
     * Respingimento non consentito da OPEN.
     *
     * @throws BusinessRuleException ticket non ancora risolto
     */
    @Override
    public void reject(Ticket ticket) {
        throw new BusinessRuleException(TicketMessage.TICKET_REJECT_NOT_RESOLVED.text());
    }

    /**
     * Risoluzione diretta non consentita da OPEN.
     *
     * @throws BusinessRuleException ticket non ancora preso in carico
     */
    @Override
    public void resolve(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_VERIFY_NOT_ASSIGNED.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAddComment() { return true; }
}
