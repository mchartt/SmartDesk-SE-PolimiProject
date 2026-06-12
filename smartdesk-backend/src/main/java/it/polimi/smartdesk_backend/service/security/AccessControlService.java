package it.polimi.smartdesk_backend.service.security;

import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;

/** Controlli di accesso su {@link AuthenticatedUser}: ruolo, titolarità risorsa, host su path, account attivo e coerenza ruolo JWT/DB. */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository users;

    /** Verifica che il ruolo del principal coincida con quello richiesto; utile su endpoint condivisi tra profili. */
    public AuthenticatedUser assertRole(AuthenticatedUser principal, Role requiredRole) {
        AuthenticatedUser requester = assertAuthenticated(principal);
        if (requester.getRole() != requiredRole) {
            throw new ForbiddenException(
                    AuthMessage.forbiddenWrongRoleForEndpoint(requester.getUserId(), requester.getRole(), requiredRole));
        }
        return requester;
    }

    /** Consente l'accesso solo al titolare della risorsa o a sys admin; {@code hiddenResourceName} compare nel messaggio di errore. */
    public AuthenticatedUser assertSelfOrAdmin(AuthenticatedUser principal, Long ownerID, String hiddenResourceName) {
        AuthenticatedUser requester = assertAuthenticated(principal);
        if (requester.getRole() == Role.SYS_ADMIN) {
            return requester;
        }
        if (!requester.getUserId().equals(ownerID)) {
            throw new ForbiddenException(AuthMessage.forbiddenResource(requester.getUserId(), hiddenResourceName));
        }
        return requester;
    }

    /** Anti-IDOR su path {@code /api/hosts/{hostID}/...}: l'ID nel path deve coincidere col subject del token. {@code resourceKind} serve solo a rendere leggibile l'errore (es. {@code "spaces"}). */
    public AuthenticatedUser assertHostOwnsPath(AuthenticatedUser principal, Long pathHostId, String resourceKind) {
        AuthenticatedUser requester = assertAuthenticated(principal);
        if (!pathHostId.equals(requester.getUserId())) {
            throw new ForbiddenException(AuthMessage.forbiddenHostResource(resourceKind, pathHostId));
        }
        return requester;
    }

    /** {@code true} se principal e ruolo DB sono entrambi {@link Role#SYS_ADMIN}. */
    public boolean isSysAdmin(AuthenticatedUser principal) {
        return assertAuthenticated(principal).getRole() == Role.SYS_ADMIN;
    }

    /** Valida principal, utente attivo, host approvato se applicabile e coerenza ruolo JWT/DB. */
    public AuthenticatedUser assertAuthenticated(AuthenticatedUser principal) {
        AuthenticatedUser authenticatedUser = requirePrincipal(principal);
        User requester = loadActiveUser(authenticatedUser.getUserId());
        if (requester.getRole() != authenticatedUser.getRole()) {
            throw new ForbiddenException(AuthMessage.FORBIDDEN_TOKEN_USER_MISMATCH.text());
        }
        return new AuthenticatedUser(requester.getId(), requester.getRole());
    }

    /** Restituisce il ruolo dopo {@link #assertAuthenticated}. */
    public Role getRoleForUser(AuthenticatedUser principal) {
        return assertAuthenticated(principal).getRole();
    }

    /** Carica user e applica regole “account vivo” (attivo + host approvato). */
    private User loadActiveUser(Long userID) {
        User user = users.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));
        if (!user.isActive()) {
            throw new ForbiddenException(AuthMessage.forbiddenUserDisabled(userID));
        }
        if (user instanceof Host host && !host.isApproved()) {
            throw new ForbiddenException(AuthMessage.forbiddenHostPending(userID));
        }
        return user;
    }

    /** Se Spring Security non ha popolato il principal → {@link UnauthorizedException}. */
    private AuthenticatedUser requirePrincipal(AuthenticatedUser principal) {
        if (principal != null) {
            return principal;
        }
        throw new UnauthorizedException(AuthMessage.AUTHENTICATION_REQUIRED.text());
    }
}
