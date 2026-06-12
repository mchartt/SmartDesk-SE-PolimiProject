package it.polimi.smartdesk_backend.model;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.admin.SystemLog;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.booking.WaitlistEntry;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketSeverity;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.user.AccountStatus;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.RefreshToken;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.service.desk.state.AvailableState;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.service.desk.state.MaintenanceState;
import it.polimi.smartdesk_backend.service.desk.state.ReservedState;

import it.polimi.smartdesk_backend.service.desk.state.DecommissionedState;
import it.polimi.smartdesk_backend.service.desk.state.PendingInspectionState;

/** Testa le classi normali nate e cresciute senza tirare in mezzo Spring, per esser più veloci. */
@FieldDefaults(level = AccessLevel.PRIVATE)
class ModelBehaviorTest {

    /** Transizioni AVAILABLE ↔ MAINTENANCE e flag {@code isBookable}. */
    @Test
    void deskStateMachineAndRestore() {
        Desk desk = new Desk();
        desk.setAmenities(List.of("monitor", "wifi"));
        DeskStateMachine stateMachine = new DeskStateMachine(List.of(
                new AvailableState(),
                new MaintenanceState(),
                new ReservedState(),
                new PendingInspectionState(),
                new DecommissionedState()));

        // Desk è AVAILABLE per default
        assertEquals(DeskStateCode.AVAILABLE, desk.getStateCode());
        assertTrue(stateMachine.isBookable(desk));

        stateMachine.markMaintenance(desk);
        assertEquals(DeskStateCode.MAINTENANCE, desk.getStateCode());
        assertFalse(stateMachine.isBookable(desk));

        stateMachine.makeAvailable(desk);
        assertEquals(DeskStateCode.PENDING_INSPECTION, desk.getStateCode());

        stateMachine.completeInspection(desk);
        assertEquals(DeskStateCode.AVAILABLE, desk.getStateCode());
    }

    /** Ruolo ereditato, stato account e {@code @PrePersist} su {@code registeredAt}. */
    @Test
    void userRoleAndPrePersist() {
        Worker worker = new Worker();
        worker.setActive(true);
        assertEquals(Role.WORKER, worker.getRole());
        assertEquals(AccountStatus.ACTIVE, worker.getStatus());

        Host host = new Host();
        host.setActive(false);
        assertEquals(Role.HOST, host.getRole());
        assertEquals(AccountStatus.SUSPENDED, host.getStatus());

        Technician technician = new Technician();
        assertEquals(Role.TECHNICIAN, technician.getRole());

        SysAdmin admin = new SysAdmin();
        assertEquals(Role.SYS_ADMIN, admin.getRole());

        assertNull(worker.getRegisteredAt());
        worker.prePersist();
        assertNotNull(worker.getRegisteredAt());

        LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 10, 9, 30);
        worker.setRegisteredAt(fixedTime);
        worker.prePersist();
        assertEquals(fixedTime, worker.getRegisteredAt());
    }

    /** Alias bio/description, {@code Host#approved}, {@code SystemLog#toAuditString}, parsing severity/status. */
    @Test
    void aliasModelsAndEnum() {
        Worker worker = new Worker();
        worker.setBio("Worker silenzioso");
        assertEquals("Worker silenzioso", worker.getBio());
        worker.setBio("Amante delle focus room");
        assertEquals("Amante delle focus room", worker.getBio());

        Technician technician = new Technician();
        technician.setSpecialization("hardware");
        assertEquals("hardware", technician.getSpecialization());

        Host host = new Host();
        host.setApproved(true);
        assertTrue(host.isApproved());
        host.setApproved(false);
        assertFalse(host.isApproved());

        SystemLog log = new SystemLog();
        log.setTimestamp(LocalDateTime.of(2026, 1, 10, 8, 0));
        log.setSeverity(LogLevel.WARN);
        log.setActorRole("WORKER");
        log.setActorID(42L);
        log.setIpAddress("127.0.0.1");
        log.setAction("BOOK_DESK");

        String auditString = log.toAuditString();
        assertTrue(auditString.contains("BOOK_DESK"));
        assertTrue(auditString.contains("WORKER/42"));

        assertEquals(TicketSeverity.MEDIUM, TicketSeverity.fromValue(null));
        assertEquals(TicketSeverity.HIGH, TicketSeverity.fromValue("high"));
        assertNotNull(assertThrows(IllegalArgumentException.class, () -> TicketSeverity.fromValue("invalid")));

        assertEquals(TicketStatus.OPEN, TicketStatus.fromValue("  "));
        assertEquals(TicketStatus.RESOLVED, TicketStatus.fromValue("resolved"));
        assertNotNull(assertThrows(IllegalArgumentException.class, () -> TicketStatus.fromValue("invalid")));
    }

    /** Timestamp ticket impostati dal service, non da {@link Ticket#report} / {@link Ticket#resolve}. */
    @Test
    void ticketTimestampSetByTheService() {
        Desk desk = new Desk();
        desk.setDeskID(10L);
        Space space = new Space();
        space.setSpaceID(3L);
        desk.setSpace(space);

        Ticket ticket = new Ticket();
        assertNull(ticket.getCreatedAt());
        ticket.report(desk);
        assertNull(ticket.getCreatedAt());

        LocalDateTime openedAt = LocalDateTime.of(2026, 5, 20, 9, 15);
        ticket.setCreatedAt(openedAt);
        assertEquals(openedAt, ticket.getCreatedAt());

        ticket.resolve("Sostituita sedia");
        assertNull(ticket.getResolvedAt());
        LocalDateTime closedAt = LocalDateTime.of(2026, 5, 21, 14, 0);
        ticket.setResolvedAt(closedAt);
        assertEquals(closedAt, ticket.getResolvedAt());
    }

    /** {@link Booking#isCompleted(LocalDateTime)} usa l'orario passato dal chiamante. */
    @Test
    void bookingCompletedWithFixedClock() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setEndTime(LocalDateTime.of(2026, 5, 20, 18, 0));

        LocalDateTime beforeEnd = LocalDateTime.of(2026, 5, 20, 17, 0);
        LocalDateTime afterEnd = LocalDateTime.of(2026, 5, 20, 19, 0);
        assertFalse(booking.isCompleted(beforeEnd));
        assertTrue(booking.isCompleted(afterEnd));
    }

    /** {@code WaitlistEntry#prePersist} non sovrascrive un {@code createdAt} già valorizzato. */
    @Test
    void waitlistTimestampDoesNotOverwriteIfAlreadySet() {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setCreatedAt(LocalDateTime.of(2026, 4, 25, 10, 0));
        entry.prePersist();
        assertEquals(LocalDateTime.of(2026, 4, 25, 10, 0), entry.getCreatedAt());

        WaitlistEntry fresh = new WaitlistEntry();
        fresh.prePersist();
        assertNotNull(fresh.getCreatedAt());
    }

    /** {@link RefreshToken#isExpired()} confronta {@code expiryDate} con l'istante corrente. */
    @Test
    void refreshTokenExpired() {
        Worker worker = new Worker();
        worker.setId(77L);
        RefreshToken token = new RefreshToken();
        token.setUser(worker);
        token.setExpiryDate(Instant.now().minusSeconds(5));
        assertTrue(token.isExpired());
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        assertFalse(token.isExpired());
    }
}
