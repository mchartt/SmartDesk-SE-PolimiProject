package it.polimi.smartdesk_backend.service.desk.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;

class DeskStateMachineTest {

    private DeskStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new DeskStateMachine(List.of(
                new AvailableState(),
                new MaintenanceState(),
                new ReservedState(),
                new PendingInspectionState(),
                new DecommissionedState()));
    }

    @Test
    void shouldAllowBookingOnAvailableDesk() {
        Desk desk = desk(DeskStateCode.AVAILABLE);

        stateMachine.assertBookable(desk);

        assertTrue(stateMachine.isBookable(desk));
    }

    @Test
    void shouldRejectBookingOnMaintenanceDesk() {
        Desk desk = desk(DeskStateCode.MAINTENANCE);

        assertThrows(BusinessRuleException.class, () -> stateMachine.assertBookable(desk));
        assertFalse(stateMachine.isBookable(desk));
    }

    @Test
    void shouldRejectBookingOnReservedDesk() {
        Desk desk = desk(DeskStateCode.RESERVED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.assertBookable(desk));
    }

    @Test
    void shouldMoveAvailableDeskToMaintenance() {
        Desk desk = desk(DeskStateCode.AVAILABLE);

        stateMachine.markMaintenance(desk);

        assertEquals(DeskStateCode.MAINTENANCE, desk.getStateCode());
        assertEquals(DeskStateCode.AVAILABLE, desk.getPreviousStateCode());
    }

    @Test
    void shouldRejectMaintenanceWhenAlreadyInMaintenance() {
        Desk desk = desk(DeskStateCode.MAINTENANCE);

        assertThrows(BusinessRuleException.class, () -> stateMachine.markMaintenance(desk));
    }

    @Test
    void shouldCompleteMaintenanceInspectionFlow() {
        Desk desk = desk(DeskStateCode.MAINTENANCE);

        stateMachine.makeAvailable(desk);
        assertEquals(DeskStateCode.PENDING_INSPECTION, desk.getStateCode());

        stateMachine.completeInspection(desk);
        assertEquals(DeskStateCode.AVAILABLE, desk.getStateCode());
    }

    @Test
    void shouldRejectExitMaintenanceFromAvailable() {
        Desk desk = desk(DeskStateCode.AVAILABLE);

        assertThrows(BusinessRuleException.class, () -> stateMachine.makeAvailable(desk));
    }

    @Test
    void shouldDecommissionDeskFromPendingInspection() {
        Desk desk = desk(DeskStateCode.PENDING_INSPECTION);

        stateMachine.decommission(desk);

        assertEquals(DeskStateCode.DECOMMISSIONED, desk.getStateCode());
    }

    @Test
    void shouldRejectDecommissionFromAvailable() {
        assertThrows(BusinessRuleException.class,
                () -> stateMachine.decommission(desk(DeskStateCode.AVAILABLE)));
    }

    @Test
    void shouldRejectOperationsOnDecommissionedDesk() {
        Desk desk = desk(DeskStateCode.DECOMMISSIONED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.assertBookable(desk));
        assertThrows(BusinessRuleException.class, () -> stateMachine.markMaintenance(desk));
        assertThrows(BusinessRuleException.class, () -> stateMachine.decommission(desk));
    }

    @Test
    void shouldRejectPendingInspectionBooking() {
        Desk desk = desk(DeskStateCode.PENDING_INSPECTION);

        assertThrows(BusinessRuleException.class, () -> stateMachine.assertBookable(desk));
    }

    @Test
    void shouldRejectDuplicateStateRegistration() {
        assertThrows(IllegalStateException.class,
                () -> new DeskStateMachine(List.of(new AvailableState(), new AvailableState())));
    }

    private static Desk desk(DeskStateCode code) {
        Desk desk = new Desk();
        desk.setDeskID(1L);
        desk.setStateCode(code);
        return desk;
    }
}
