package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

/** VERIFYING: attesa approvazione host; approva → RESOLVED, respinge → IN_PROGRESS. */
@Component
public class VerifyingTicketState implements TicketState {

    /** {@inheritDoc} */
    @Override
    public TicketStatus code() { return TicketStatus.VERIFYING; }

    /** Scrive VERIFYING sul ticket. */
    @Override
    public void enter(Ticket ticket) {
        ticket.setStatus(TicketStatus.VERIFYING.name());
    }

    /** Ri-assegna il tecnico e torna in lavorazione. */
    @Override
    public void assignTechnician(Ticket ticket, Long technicianId) {
        // Ri-assegnazione ammessa se host vuole cambiare tecnico per la nuova manutenzione
        ticket.setTechnicianID(technicianId);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
    }

    /** Aggiorna la risoluzione se il tecnico corregge il testo. */
    @Override
    public void verify(Ticket ticket, String resolution) {
        // Già in verifica, aggiorniamo solo la risoluzione se necessario
        ticket.setResolution(resolution);
    }

    /** Host approva → RESOLVED. */
    @Override
    public void approve(Ticket ticket) {
        ticket.setStatus(TicketStatus.RESOLVED.name());
    }

    /** Host respinge → torna IN_PROGRESS. */
    @Override
    public void reject(Ticket ticket) {
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
    }

    /**
     * Risoluzione diretta non consentita da VERIFYING.
     *
     * @throws BusinessRuleException già in verifica
     */
    @Override
    public void resolve(Ticket ticket, String resolution) {
        throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_VERIFYING.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAddComment() { return true; }
}
