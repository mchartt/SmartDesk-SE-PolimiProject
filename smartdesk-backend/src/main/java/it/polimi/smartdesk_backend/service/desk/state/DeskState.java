package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.util.message.DeskMessage;

/** Comportamento per {@link DeskStateCode}: transizioni manutenzione e gate sulla creazione prenotazione. */
public interface DeskState {

    /** Codice stato persistito sul desk. */
    DeskStateCode code();

    /** Aggiorna {@code stateCode} sul desk quando entri in questo stato. */
    void enter(Desk desk);

    /**
     * Gate pre-creazione prenotazione.
     *
     * @throws BusinessRuleException desk in MAINTENANCE, RESERVED o transizione non valida
     */
    void attemptBooking(Desk desk);

    /**
     * Transizione verso MAINTENANCE.
     *
     * @throws BusinessRuleException transizione verso MAINTENANCE non ammessa (es. già in manutenzione)
     */
    void onEnterMaintenance(Desk desk);

    /**
     * Uscita da MAINTENANCE.
     *
     * @throws BusinessRuleException uscita manutenzione non ammessa (es. desk già AVAILABLE)
     */
    void onExitMaintenance(Desk desk);

    /**
     * Ispezione host completata (PENDING_INSPECTION → AVAILABLE).
     *
     * @throws BusinessRuleException transizione non ammessa da questo stato
     */
    default void onInspectionComplete(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_OPERATION_NOT_ALLOWED.text());
    }

    /**
     * Dismissione definitiva della postazione.
     *
     * @throws BusinessRuleException transizione non ammessa da questo stato
     */
    default void decommission(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_DECOMMISSION_ONLY_AVAILABLE_OR_INSPECTION.text());
    }

    /** {@code false} se la postazione non accetta nuove prenotazioni a livello stato. */
    default boolean isBookable() {
        return true;
    }
}
