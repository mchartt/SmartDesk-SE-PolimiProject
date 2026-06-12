package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** State machine ticket: non cambiare status a mano nel service, passa da qui (OPEN, IN_PROGRESS, VERIFYING, …). */
@Component
public class TicketStateMachine {

    private final Map<TicketStatus, TicketState> statesByCode;

    /** Indicizza gli stati Spring e fallisce se manca un {@link TicketStatus}. */
    public TicketStateMachine(List<TicketState> states) {
        Map<TicketStatus, TicketState> indexedStates = new EnumMap<>(TicketStatus.class);
        for (TicketState state : states) {
            TicketState previous = indexedStates.put(state.code(), state);
            if (previous != null) {
                throw new IllegalStateException("Duplicate ticket state: " + state.code());
            }
        }
        for (TicketStatus code : TicketStatus.values()) {
            if (!indexedStates.containsKey(code)) {
                throw new IllegalStateException("Missing ticket state: " + code);
            }
        }
        this.statesByCode = Map.copyOf(indexedStates);
    }

    private TicketState get(TicketStatus code) {
        TicketState state = statesByCode.get(code);
        if (state == null) {
            throw new IllegalArgumentException("Unsupported ticket status: " + code);
        }
        return state;
    }

    /** Assegna tecnico (tipicamente da OPEN). */
    public void assignTechnician(Ticket ticket, Long technicianId) {
        get(TicketStatus.fromValue(ticket.getStatus()))
                .assignTechnician(ticket, technicianId);
    }

    /** Tecnico manda in attesa approvazione host (serve testo resolution). */
    public void verify(Ticket ticket, String resolution) {
        get(TicketStatus.fromValue(ticket.getStatus()))
                .verify(ticket, resolution);
    }

    /** Host ok → RESOLVED. */
    public void approve(Ticket ticket) {
        get(TicketStatus.fromValue(ticket.getStatus()))
                .approve(ticket);
    }

    /** Host non convinto → torna in lavorazione. */
    public void reject(Ticket ticket) {
        get(TicketStatus.fromValue(ticket.getStatus()))
                .reject(ticket);
    }

    /** Chiusura diretta in RESOLVED (salta la verifica host se il flusso lo permette). */
    public void resolve(Ticket ticket, String resolution) {
        get(TicketStatus.fromValue(ticket.getStatus()))
                .resolve(ticket, resolution);
    }

    /** false se il ticket è già chiuso — il worker non può più commentare/eliminare. */
    public boolean canAddComment(Ticket ticket) {
        return get(TicketStatus.fromValue(ticket.getStatus()))
                .canAddComment();
    }

    /** Chiusura host (es. desk dismesso): non consentita da RESOLVED/CLOSED. */
    public void close(Ticket ticket) {
        TicketStatus current = TicketStatus.fromValue(ticket.getStatus());
        if (current == TicketStatus.CLOSED || current == TicketStatus.RESOLVED) {
            throw new BusinessRuleException(TicketMessage.TICKET_ALREADY_CLOSED_OR_RESOLVED.text());
        }
        get(TicketStatus.CLOSED).enter(ticket);
    }
}
