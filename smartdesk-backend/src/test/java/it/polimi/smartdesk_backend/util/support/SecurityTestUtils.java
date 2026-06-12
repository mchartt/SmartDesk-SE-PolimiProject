package it.polimi.smartdesk_backend.util.support;

import it.polimi.smartdesk_backend.util.message.AuthMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;

import java.security.Principal;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.experimental.UtilityClass;

/** Helper per test MVC: principal finti e mock di {@link AccessControlService}. */
@UtilityClass
public class SecurityTestUtils {

    public static Principal authenticatedUser(long userId, Role role) {
        AuthenticatedUser user = EntityTestFixtures.principal(userId, role);
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(authority(role)));
    }

    public static void stubAuthenticated(AccessControlService accessControlService, long userId, Role role) {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(EntityTestFixtures.principal(userId, role));
    }

    public static void stubHostPathAccess(AccessControlService accessControlService, long hostUserId, Role role) {
        AuthenticatedUser user = EntityTestFixtures.principal(hostUserId, role);
        when(accessControlService.assertAuthenticated(any())).thenReturn(user);
        doAnswer(invocation -> {
            Long pathHostId = invocation.getArgument(1);
            String resourceKind = invocation.getArgument(2);
            if (!pathHostId.equals(hostUserId)) {
                throw new ForbiddenException(AuthMessage.forbiddenHostResource(resourceKind, pathHostId));
            }
            return user;
        }).when(accessControlService).assertHostOwnsPath(any(), anyLong(), anyString());
    }

    private static SimpleGrantedAuthority authority(Role role) {
        return new SimpleGrantedAuthority("ROLE_" + role.name());
    }
}

