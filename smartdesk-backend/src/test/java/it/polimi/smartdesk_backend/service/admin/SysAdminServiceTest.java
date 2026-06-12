package it.polimi.smartdesk_backend.service.admin;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

/** Azioni amministratore: moderazione utenti, host, spazi e consultazione log. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class SysAdminServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private HostRepository hostRepo;

    @Mock
    private SpaceRepository spaceRepo;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SysAdminService sysAdminService;

    @Test
    void banUser_selfBanThrows() {
        assertThrows(BusinessRuleException.class, () -> sysAdminService.banUser(5L, 5L, "10.0.0.5"));
    }

    @Test
    void banAndReactivateUser() {
        Worker worker = new Worker();
        worker.setId(8L);
        worker.setActive(true);
        when(userRepo.findById(8L)).thenReturn(Optional.of(worker));

        sysAdminService.banUser(8L, "10.0.0.1");
        assertFalse(worker.isActive());
        verify(auditLogService).log(Role.SYS_ADMIN, null, "Utente disattivato: 8", LogLevel.AUDIT, "10.0.0.1");

        sysAdminService.reactivateUser(8L, "10.0.0.2");
        assertTrue(worker.isActive());
        verify(auditLogService).log(Role.SYS_ADMIN, null, "Utente riattivato: 8", LogLevel.AUDIT, "10.0.0.2");
    }

    @Test
    void approveRejectHostAndApproveForceCloseSpace() {
        Host host = new Host();
        host.setId(4L);
        host.setApproved(false);
        host.setActive(true);

        Space space = new Space();
        space.setSpaceID(11L);
        space.setApproved(false);

        when(hostRepo.findById(4L)).thenReturn(Optional.of(host));
        when(spaceRepo.findById(11L)).thenReturn(Optional.of(space));

        sysAdminService.approveHost(4L, "1.1.1.1");
        assertTrue(host.isApproved());

        sysAdminService.rejectHost(4L, "1.1.1.2");
        assertFalse(host.isApproved());
        assertFalse(host.isActive());

        sysAdminService.approveSpace(11L, "2.2.2.2");
        assertTrue(space.isApproved());

        sysAdminService.forceCloseSpace(11L, "2.2.2.3");
        assertFalse(space.isApproved());

        verify(auditLogService).log(Role.SYS_ADMIN, null, "Host approvato: 4", LogLevel.AUDIT, "1.1.1.1");
        verify(auditLogService).log(Role.SYS_ADMIN, null, "Host rifiutato: 4", LogLevel.WARN, "1.1.1.2");
        verify(auditLogService).log(Role.SYS_ADMIN, null, "Spazio approvato: 11", LogLevel.AUDIT, "2.2.2.2");
        verify(auditLogService).log(Role.SYS_ADMIN, null, "Spazio chiuso forzatamente: 11", LogLevel.AUDIT, "2.2.2.3");
        verify(notificationService).notifySpaceDecision(space.getHostID(), space.getName(), "approvato");
        verify(notificationService).notifySpaceDecision(space.getHostID(), space.getName(), "chiuso forzatamente");
    }

    @Test
    void adminNotFoundMissingEntities() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        when(hostRepo.findById(99L)).thenReturn(Optional.empty());
        when(spaceRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sysAdminService.banUser(99L, "ip"));
        assertThrows(NotFoundException.class, () -> sysAdminService.approveHost(99L, "ip"));
        assertThrows(NotFoundException.class, () -> sysAdminService.approveSpace(99L, "ip"));
    }

    @Test
    void adminUsersPendingHostAndLog() {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setName("Worker");
        worker.setEmail("worker@example.com");
        worker.setActive(true);
        worker.setRegisteredAt(LocalDateTime.of(2026, 4, 25, 10, 0));

        Host pendingHost = new Host();
        pendingHost.setId(2L);
        pendingHost.setName("Host");
        pendingHost.setSurname("Rossi");
        pendingHost.setEmail("host@example.com");
        pendingHost.setActive(true);
        pendingHost.setApproved(false);
        pendingHost.setDescription("Gestiamo uno spazio di coworking condiviso.");
        pendingHost.setNameStructure("HostOrg");

        Host approvedHost = new Host();
        approvedHost.setId(3L);
        approvedHost.setActive(true);
        approvedHost.setApproved(true);

        when(userRepo.findAll()).thenReturn(List.of(worker, pendingHost, approvedHost));
        when(hostRepo.findAll()).thenReturn(List.of(pendingHost, approvedHost));

        LogDTO log = new LogDTO();
        log.setLogID(77L);
        when(auditLogService.getLogs()).thenReturn(List.of(log));

        List<UserProfileDTO> allUsers = sysAdminService.getAllUsers();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().noneMatch(u -> Long.valueOf(2L).equals(u.getUserID())));
        assertTrue(allUsers.stream().anyMatch(u -> Long.valueOf(1L).equals(u.getUserID())));
        assertTrue(allUsers.stream().anyMatch(u -> Long.valueOf(3L).equals(u.getUserID())));

        List<UserProfileDTO> pendingHosts = sysAdminService.getPendingHosts();
        assertEquals(1, pendingHosts.size());
        assertFalse(pendingHosts.get(0).isApproved());
        assertEquals("Gestiamo uno spazio di coworking condiviso.", pendingHosts.get(0).getDescription());
        assertEquals("HostOrg", pendingHosts.get(0).getNameStructure());
        assertEquals("Rossi", pendingHosts.get(0).getSurname());

        assertEquals(77L, sysAdminService.getSystemLogs().get(0).getLogID());
    }

    @Test
    void pendingHostEmptyList() {
        Host approvedOne = new Host();
        approvedOne.setId(10L);
        approvedOne.setApproved(true);
        approvedOne.setActive(true);
        Host approvedTwo = new Host();
        approvedTwo.setId(11L);
        approvedTwo.setApproved(true);
        approvedTwo.setActive(true);
        when(hostRepo.findAll()).thenReturn(List.of(approvedOne, approvedTwo));

        List<UserProfileDTO> pendingHosts = sysAdminService.getPendingHosts();

        assertTrue(pendingHosts.isEmpty());
    }
}
