package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.util.message.DeskMessage;
import org.springframework.stereotype.Component;
import lombok.NoArgsConstructor;

/** AVAILABLE: prenotazione consentita a livello stato; overlap orario su {@code booking}. */
@Component
@NoArgsConstructor
public class AvailableState implements DeskState {

    /** {@inheritDoc} */
    @Override
    public DeskStateCode code() {
        return DeskStateCode.AVAILABLE;
    }

    /** Scrive AVAILABLE sul desk. */
    @Override
    public void enter(Desk desk) {
        desk.setStateCode(code());
    }

    /** Nessun blocco: la disponibilità reale la controllano i booking. */
    @Override
    public void attemptBooking(Desk desk) {
    }

    /** Autorizza il passaggio in manutenzione. */
    @Override
    public void onEnterMaintenance(Desk desk) {
    }

    /**
     * Uscita da manutenzione non consentita da AVAILABLE.
     *
     * @throws BusinessRuleException il desk non è in manutenzione
     */
    @Override
    public void onExitMaintenance(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_NOT_IN_MAINTENANCE.text());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBookable() {
        return true;
    }
}
