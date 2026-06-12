package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.util.message.BookingMessage;
import it.polimi.smartdesk_backend.util.message.DeskMessage;
import org.springframework.stereotype.Component;
import lombok.NoArgsConstructor;

/** MAINTENANCE: {@link it.polimi.smartdesk_backend.util.message.BookingMessage#DESK_UNDER_MAINTENANCE} su ogni tentativo di prenotazione. */
@Component
@NoArgsConstructor
public class MaintenanceState implements DeskState {

    /** {@inheritDoc} */
    @Override
    public DeskStateCode code() {
        return DeskStateCode.MAINTENANCE;
    }

    /** Scrive MAINTENANCE sul desk. */
    @Override
    public void enter(Desk desk) {
        desk.setStateCode(code());
    }

    /**
     * Prenotazione bloccata in MAINTENANCE.
     *
     * @throws BusinessRuleException postazione in manutenzione
     */
    @Override
    public void attemptBooking(Desk desk) {
        throw new BusinessRuleException(BookingMessage.DESK_UNDER_MAINTENANCE.text());
    }

    /**
     * Transizione verso MAINTENANCE non consentita: già in manutenzione.
     *
     * @throws BusinessRuleException già in manutenzione
     */
    @Override
    public void onEnterMaintenance(Desk desk) {
        throw new BusinessRuleException(DeskMessage.DESK_ALREADY_IN_MAINTENANCE.text());
    }

    /** Autorizza l'uscita verso ispezione (gestita da {@link DeskStateMachine}). */
    @Override
    public void onExitMaintenance(Desk desk) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBookable() {
        return false;
    }
}
