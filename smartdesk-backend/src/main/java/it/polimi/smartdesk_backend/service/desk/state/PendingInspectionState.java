package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.util.message.DeskMessage;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/** PENDING_INSPECTION: la manutenzione è completata ma l'host deve approvare il ripristino prima che la postazione torni prenotabile. Transizioni ammesse: → AVAILABLE        (host approva: onInspectionComplete) → MAINTENANCE      (ispezione fallisce: onEnterMaintenance) → DECOMMISSIONED   (host dismette: decommission) */
@Component
@NoArgsConstructor
public class PendingInspectionState implements DeskState {

    /** {@inheritDoc} */
    @Override
    public DeskStateCode code() { return DeskStateCode.PENDING_INSPECTION; }

    /** Scrive PENDING_INSPECTION sul desk. */
    @Override
    public void enter(Desk desk) {
        desk.setStateCode(code());
    }

    /**
     * Prenotazione bloccata in attesa di ispezione host.
     *
     * @throws BusinessRuleException in attesa di ispezione host
     */
    @Override
    public void attemptBooking(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_PENDING_INSPECTION.text());
    }

    /** Autorizza il ritorno in manutenzione se l'ispezione fallisce. */
    @Override
    public void onEnterMaintenance(Desk desk) {
        // L'ispezione è fallita: il tecnico può rimetterla in manutenzione.
        // La transizione effettiva verso MAINTENANCE la gestisce DeskStateMachine.
    }

    /**
     * Uscita da manutenzione non consentita da PENDING_INSPECTION.
     *
     * @throws BusinessRuleException non è in manutenzione
     */
    @Override
    public void onExitMaintenance(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_NOT_IN_MAINTENANCE_SHORT.text());
    }

    /** Autorizza il passaggio ad AVAILABLE (la macchina fa moveTo). */
    @Override
    public void onInspectionComplete(Desk desk) {
        // No-op: autorizza la transizione. DeskStateMachine chiama moveTo(AVAILABLE).
    }

    /** Autorizza la dismissione (la macchina fa moveTo). */
    @Override
    public void decommission(Desk desk) {
        // No-op: autorizza la transizione. DeskStateMachine chiama moveTo(DECOMMISSIONED).
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBookable() { return false; }
}
