package it.polimi.smartdesk_backend.service.security;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

/** Controlli centralizzati su ruolo, utente attivo e appartenenza host alle risorse. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccessControlService accessControlService;

    @Test
    void whenRoleCorrectDoesNotBlock() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.worker(id)));

        assertDoesNotThrow(() -> accessControlService.assertRole(auth(id, Role.WORKER), Role.WORKER));
    }

    @Test
    void wrongRoleThrowsForbidden() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.worker(id)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.assertRole(auth(id, Role.WORKER), Role.SYS_ADMIN));
    }

    @Test
    void resourceOwnerPassesSelfCheck() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.worker(id)));

        assertDoesNotThrow(() -> accessControlService.assertSelfOrAdmin(auth(id, Role.WORKER), id, "Ticket 12"));
    }

    @Test
    void adminBypassesSelfCheck() {
        long adminId = 1L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(EntityTestFixtures.admin(adminId)));

        assertDoesNotThrow(
                () -> accessControlService.assertSelfOrAdmin(auth(adminId, Role.SYS_ADMIN), 4L, "Ticket 12"));
    }

    @Test
    void otherWorkerDoesNotPassSelfCheck() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.worker(id)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.assertSelfOrAdmin(auth(id, Role.WORKER), 99L, "Ticket 12"));
    }

    @Test
    void hostIdInPathMatchesOk() {
        long hostId = 8L;
        when(userRepository.findById(hostId)).thenReturn(Optional.of(EntityTestFixtures.host(hostId, true)));

        assertDoesNotThrow(
                () -> accessControlService.assertHostOwnsPath(auth(hostId, Role.HOST), hostId, "spaces"));
    }

    @Test
    void hostIdInPathOfAnotherForbidden() {
        long hostId = 8L;
        when(userRepository.findById(hostId)).thenReturn(Optional.of(EntityTestFixtures.host(hostId, true)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.assertHostOwnsPath(auth(hostId, Role.HOST), 99L, "reviews"));
    }

    @Test
    void onlyAdminRecognizedAsSysAdmin() {
        long adminId = 1L;
        long workerId = 4L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(EntityTestFixtures.admin(adminId)));
        when(userRepository.findById(workerId)).thenReturn(Optional.of(EntityTestFixtures.worker(workerId)));

        assertTrue(accessControlService.isSysAdmin(auth(adminId, Role.SYS_ADMIN)));
        assertFalse(accessControlService.isSysAdmin(auth(workerId, Role.WORKER)));
    }

    @Test
    void disabledWorkerDoesNotAuthenticate() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.workerBanned(id)));

        assertThrows(ForbiddenException.class, () -> accessControlService.assertAuthenticated(auth(id, Role.WORKER)));
    }

    @Test
    void correctRoleButAccountOffForbidden() {
        long id = 12L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.workerBanned(id)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.assertRole(auth(id, Role.WORKER), Role.WORKER));
    }

    @Test
    void hostNotApprovedStaysOut() {
        long hostId = 6L;
        when(userRepository.findById(hostId)).thenReturn(Optional.of(EntityTestFixtures.host(hostId, false)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.assertAuthenticated(auth(hostId, Role.HOST)));
    }

    @Test
    void jwtWithRoleDifferentFromDbForbidden() {
        long id = 4L;
        when(userRepository.findById(id)).thenReturn(Optional.of(EntityTestFixtures.worker(id)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.getRoleForUser(auth(id, Role.SYS_ADMIN)));
    }

    @Test
    void principalNullUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> accessControlService.assertAuthenticated(null));
    }

    private AuthenticatedUser auth(long userId, Role role) {
        return EntityTestFixtures.principal(userId, role);
    }
}
