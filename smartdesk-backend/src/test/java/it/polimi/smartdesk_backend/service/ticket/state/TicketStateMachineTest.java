package it.polimi.smartdesk_backend.service.ticket.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;

class TicketStateMachineTest {

    private TicketStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TicketStateMachine(List.of(
                new OpenTicketState(),
                new InProgressTicketState(),
                new VerifyingTicketState(),
                new ResolvedTicketState(),
                new ClosedTicketState()));
    }

    @Test
    void shouldAssignTechnicianFromOpen() {
        Ticket ticket = ticket(TicketStatus.OPEN);

        stateMachine.assignTechnician(ticket, 9L);

        assertEquals(TicketStatus.IN_PROGRESS.name(), ticket.getStatus());
        assertEquals(9L, ticket.getTechnicianID());
    }

    @Test
    void shouldRejectVerifyFromOpen() {
        Ticket ticket = ticket(TicketStatus.OPEN);

        assertThrows(BusinessRuleException.class, () -> stateMachine.verify(ticket, "done"));
    }

    @Test
    void shouldMoveInProgressToVerifying() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

        stateMachine.verify(ticket, "Cavo riparato");

        assertEquals(TicketStatus.VERIFYING.name(), ticket.getStatus());
        assertEquals("Cavo riparato", ticket.getResolution());
    }

    @Test
    void shouldResolveDirectlyFromInProgress() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

        stateMachine.resolve(ticket, "Resolved without host");

        assertEquals(TicketStatus.RESOLVED.name(), ticket.getStatus());
    }

    @Test
    void shouldApproveFromVerifying() {
        Ticket ticket = ticket(TicketStatus.VERIFYING);

        stateMachine.approve(ticket);

        assertEquals(TicketStatus.RESOLVED.name(), ticket.getStatus());
    }

    @Test
    void shouldRejectFromVerifyingBackToInProgress() {
        Ticket ticket = ticket(TicketStatus.VERIFYING);

        stateMachine.reject(ticket);

        assertEquals(TicketStatus.IN_PROGRESS.name(), ticket.getStatus());
    }

    @Test
    void shouldReassignTechnicianFromVerifying() {
        Ticket ticket = ticket(TicketStatus.VERIFYING);
        ticket.setTechnicianID(3L);

        stateMachine.assignTechnician(ticket, 8L);

        assertEquals(8L, ticket.getTechnicianID());
        assertEquals(TicketStatus.IN_PROGRESS.name(), ticket.getStatus());
    }

    @Test
    void shouldRejectMutationsOnResolved() {
        Ticket ticket = ticket(TicketStatus.RESOLVED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.assignTechnician(ticket, 1L));
        assertFalse(stateMachine.canAddComment(ticket));
    }

    @Test
    void shouldCloseFromOpen() {
        Ticket ticket = ticket(TicketStatus.OPEN);

        stateMachine.close(ticket);

        assertEquals(TicketStatus.CLOSED.name(), ticket.getStatus());
    }

    @Test
    void shouldRejectCloseWhenAlreadyResolved() {
        Ticket ticket = ticket(TicketStatus.RESOLVED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.close(ticket));
    }

    @Test
    void shouldRejectCloseWhenAlreadyClosed() {
        Ticket ticket = ticket(TicketStatus.CLOSED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.close(ticket));
    }

    @Test
    void shouldRejectMutationsOnClosed() {
        Ticket ticket = ticket(TicketStatus.CLOSED);

        assertThrows(BusinessRuleException.class, () -> stateMachine.resolve(ticket, "x"));
        assertFalse(stateMachine.canAddComment(ticket));
    }

    @Test
    void shouldAllowCommentsOnOpenTicket() {
        assertTrue(stateMachine.canAddComment(ticket(TicketStatus.OPEN)));
    }

    @Test
    void shouldRejectDuplicateStateRegistration() {
        assertThrows(IllegalStateException.class,
                () -> new TicketStateMachine(List.of(new OpenTicketState(), new OpenTicketState())));
    }

    private static Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTicketID(1L);
        ticket.setStatus(status.name());
        return ticket;
    }
}
