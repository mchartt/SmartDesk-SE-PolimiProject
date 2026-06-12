package it.polimi.smartdesk_backend.service.ticket.state;

import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;

/** Comportamento per {@link TicketStatus}: transizioni e gate su commenti. */
public interface TicketState {
    /** Stato persistito sul ticket. */
    TicketStatus code();

    /** Aggiorna {@code status} sul ticket quando entri in questo stato. */
    void enter(Ticket ticket);

    /**
     * Assegna un tecnico al ticket.
     *
     * @throws it.polimi.smartdesk_backend.exception.BusinessRuleException se la transizione non è in {@link TicketStatus#allowedFrom(TicketStatus)}
     */
    void assignTechnician(Ticket ticket, Long technicianId);

    /**
     * Passa il ticket in verifica host con testo di risoluzione.
     *
     * @param resolution testo risoluzione persistito sul ticket
     * @throws it.polimi.smartdesk_backend.exception.BusinessRuleException se non si può passare in verifica da questo stato
     */
    void verify(Ticket ticket, String resolution);

    /**
     * Approvazione host della risoluzione proposta.
     *
     * @throws it.polimi.smartdesk_backend.exception.BusinessRuleException se non si può approvare da questo stato
     */
    void approve(Ticket ticket);

    /**
     * Respinge la risoluzione e rimanda il ticket in lavorazione.
     *
     * @throws it.polimi.smartdesk_backend.exception.BusinessRuleException se non si può respingere da questo stato
     */
    void reject(Ticket ticket);

    /**
     * Risoluzione diretta del ticket senza passaggio in verifica host.
     *
     * @param resolution testo risoluzione
     * @throws it.polimi.smartdesk_backend.exception.BusinessRuleException se non si può risolvere direttamente da questo stato
     */
    void resolve(Ticket ticket, String resolution);

    /** {@code false} su RESOLVED/CLOSED. */
    boolean canAddComment();
}
