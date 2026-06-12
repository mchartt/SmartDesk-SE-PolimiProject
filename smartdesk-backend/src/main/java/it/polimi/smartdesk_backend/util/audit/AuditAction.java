package it.polimi.smartdesk_backend.util.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Azioni tracciabili nel log di audit. */
@Getter
@RequiredArgsConstructor
public enum AuditAction {
    USER_REGISTERED("Utente registrato"),
    HOST_REGISTERED_PENDING("Host registrato (in attesa di approvazione)"),
    USER_LOGGED_IN("Accesso effettuato"),
    REFRESH_TOKEN_USED("Token di refresh utilizzato"),
    USER_LOGGED_OUT("Logout effettuato"),
    PROFILE_UPDATED("Profilo aggiornato"),
    PASSWORD_CHANGED("Password modificata"),
    ACCOUNT_DEACTIVATED_BY_USER("Account disattivato (richiesta utente)");

    private final String description;
}
