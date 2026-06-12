package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.util.message.DeskMessage;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/** DECOMMISSIONED: stato terminale. La postazione è definitivamente dismessa dall'host. Nessuna transizione di uscita è ammessa. */
@Component
@NoArgsConstructor
public class DecommissionedState implements DeskState {

    /** {@inheritDoc} */
    @Override
    public DeskStateCode code() { return DeskStateCode.DECOMMISSIONED; }

    /** Scrive DECOMMISSIONED sul desk. */
    @Override
    public void enter(Desk desk) {
        desk.setStateCode(code());
    }

    /**
     * Prenotazione bloccata su postazione dismessa.
     *
     * @throws BusinessRuleException postazione dismessa
     */
    @Override
    public void attemptBooking(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_DECOMMISSIONED_NOT_BOOKABLE.text());
    }

    /**
     * Transizione verso MAINTENANCE non consentita da DECOMMISSIONED.
     *
     * @throws BusinessRuleException non si può mettere in manutenzione una postazione dismessa
     */
    @Override
    public void onEnterMaintenance(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_DECOMMISSION_CANNOT_MAINTENANCE.text());
    }

    /**
     * Uscita da manutenzione non consentita: stato terminale.
     *
     * @throws BusinessRuleException stato terminale
     */
    @Override
    public void onExitMaintenance(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_DECOMMISSION_CANNOT_RESTORE.text());
    }

    /**
     * Dismissione ripetuta non consentita.
     *
     * @throws BusinessRuleException già dismessa
     */
    @Override
    public void decommission(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_ALREADY_DECOMMISSIONED.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBookable() { return false; }
}
