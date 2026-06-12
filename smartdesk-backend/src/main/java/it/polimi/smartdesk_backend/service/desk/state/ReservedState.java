package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.util.message.DeskMessage;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/** RESERVED: blocco temporaneo (es. checkout); prenotazione e {@code isBookable} = false. */
@Component
@NoArgsConstructor
public class ReservedState implements DeskState {

    /** {@inheritDoc} */
    @Override
    public DeskStateCode code() {
        return DeskStateCode.RESERVED;
    }

    /** Scrive RESERVED sul desk. */
    @Override
    public void enter(Desk desk) {
        desk.setStateCode(code());
    }

    /**
     * Prenotazione bloccata in RESERVED.
     *
     * @throws BusinessRuleException postazione riservata
     */
    @Override
    public void attemptBooking(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_TEMPORARILY_RESERVED.text());
    }

    /** Autorizza il passaggio in manutenzione. */
    @Override
    public void onEnterMaintenance(Desk desk) {
    }

    /**
     * Uscita da manutenzione non consentita da RESERVED.
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
        return false;
    }
}
