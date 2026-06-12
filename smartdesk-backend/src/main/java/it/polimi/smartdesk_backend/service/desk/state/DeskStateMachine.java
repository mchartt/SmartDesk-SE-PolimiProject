package it.polimi.smartdesk_backend.service.desk.state;

import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Facciata State su {@link DeskState}: prenotabilità e transizioni AVAILABLE/MAINTENANCE/RESERVED. I service non istanziano stati concreti; la disponibilità per giorno resta sui booking. */
@Component
public class DeskStateMachine {

    private final Map<DeskStateCode, DeskState> statesByCode;

    /** Indicizza gli stati Spring e fallisce se manca un {@link DeskStateCode}. */
    public DeskStateMachine(List<DeskState> states) {
        Map<DeskStateCode, DeskState> indexedStates = new EnumMap<>(DeskStateCode.class);
        for (DeskState state : states) {
            DeskState previous = indexedStates.put(state.code(), state);
            if (previous != null) {
                throw new IllegalStateException("Duplicate desk state: " + state.code());
            }
        }
        for (DeskStateCode code : DeskStateCode.values()) {
            if (!indexedStates.containsKey(code)) {
                throw new IllegalStateException("Missing desk state: " + code);
            }
        }
        this.statesByCode = Map.copyOf(indexedStates);
    }

    /** Stato corrente del desk in base a {@code stateCode}. */
    public DeskState currentStateOf(Desk desk) {
        return get(desk.getStateCode());
    }

    /** Applica {@link DeskState#enter(Desk)} per lo stato target. */
    public void moveTo(Desk desk, DeskStateCode targetState) {
        get(targetState).enter(desk);
    }

    private DeskState get(DeskStateCode code) {
        DeskState state = statesByCode.get(code);
        if (state == null) {
            throw new IllegalArgumentException("Unsupported desk state: " + code);
        }
        return state;
    }

    /** Verifica prenotabilità operativa; {@link it.polimi.smartdesk_backend.exception.BusinessRuleException} se MAINTENANCE o RESERVED (messaggio dallo stato concreto). */
    public void assertBookable(Desk desk) {
        currentStateOf(desk).attemptBooking(desk);
    }

    /** MAINTENANCE dopo {@link DeskState#onEnterMaintenance}; rifiuta se già in manutenzione. */
    public void markMaintenance(Desk desk) {
        currentStateOf(desk).onEnterMaintenance(desk);
        desk.setPreviousStateCode(desk.getStateCode());
        moveTo(desk, DeskStateCode.MAINTENANCE);
    }

    /** MAINTENANCE → PENDING_INSPECTION: il tecnico ha completato il lavoro. */
    public void makeAvailable(Desk desk) {
        currentStateOf(desk).onExitMaintenance(desk);
        moveTo(desk, DeskStateCode.PENDING_INSPECTION);
        desk.setPreviousStateCode(null);
    }

    /** PENDING_INSPECTION → AVAILABLE: l'host approva il ripristino. */
    public void completeInspection(Desk desk) {
        currentStateOf(desk).onInspectionComplete(desk);
        moveTo(desk, DeskStateCode.AVAILABLE);
    }

    /** → DECOMMISSIONED: l'host dismette la postazione. */
    public void decommission(Desk desk) {
        currentStateOf(desk).decommission(desk);
        moveTo(desk, DeskStateCode.DECOMMISSIONED);
    }

    /** {@code false} in manutenzione; non sostituisce il controllo overlap prenotazioni. */
    public boolean isBookable(Desk desk) {
        return currentStateOf(desk).isBookable();
    }
}
