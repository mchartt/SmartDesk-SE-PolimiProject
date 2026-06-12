package it.polimi.smartdesk_backend.util.message;

import it.polimi.smartdesk_backend.model.user.Role;
import lombok.RequiredArgsConstructor;

/** Testi per login, registrazione, refresh JWT e errori di permessi. I factory statici aggiungono id/ruolo quando serve contesto nel path. */
@RequiredArgsConstructor
public enum AuthMessage {
    EMAIL_ALREADY_REGISTERED("Questa email è già registrata."),
    INVALID_CREDENTIALS("Email o password non corretti."),
    USER_DISABLED("Account disattivato."),
    HOST_PENDING("Account host in attesa di approvazione dell'amministratore."),
    REFRESH_TOKEN_NOT_FOUND("Sessione non trovata: rifai il login."),
    REFRESH_TOKEN_STALE("Il refresh token non è più valido."),
    TECHNICIAN_REGISTER_VIA_HOST("Il tecnico va creato dal pannello host."),
    SYS_ADMIN_REGISTER_FORBIDDEN("Non è possibile registrare un SYS_ADMIN da endpoint pubblico."),
    HOST_REGISTER_USE_DEDICATED("Per registrare un host usa POST /api/auth/register/host."),
    UNSUPPORTED_ROLE("Ruolo non supportato."),
    ROLE_REQUIRED("Il ruolo è obbligatorio."),
    AUTHENTICATION_REQUIRED("Autenticazione richiesta."),
    FORBIDDEN_TOKEN_USER_MISMATCH("Accesso negato: il token non corrisponde all'utente autenticato."),
    TOKEN_MISSING("Token mancante o header Authorization non valido."),
    TOKEN_INVALID_OR_EXPIRED("Token non valido o scaduto."),
    TOKEN_INVALID("Token non valido."),
    TOKEN_INVALID_ISSUER("Token con emittente non valido."),
    TOKEN_INVALID_PAYLOAD("Token con payload non valido."),
    TOKEN_REVOKED("Questo token è stato revocato."),
    UNAUTHORIZED_FALLBACK("Non autorizzato."),
    ACCESS_DENIED("Accesso negato.");

    private final String text;

    public String text() {
        return text;
    }

    /** Messaggio dinamico per ruolo non riconosciuto nel JSON. */
    public static String unsupportedRoleValue(String value) {
        return "Ruolo non supportato: " + value;
    }

    /** Utente con ruolo sbagliato rispetto alla risorsa richiesta. */
    public static String forbiddenWrongRole(long userId, String role) {
        return "Accesso negato: l'utente " + userId + " ha ruolo " + role + ".";
    }

    public static String forbiddenResource(long userId, String resource) {
        return "Accesso negato: l'utente " + userId + " non può accedere a " + resource + ".";
    }

    /** Dettaglio ruolo attuale vs atteso per endpoint protetti. */
    public static String forbiddenWrongRoleForEndpoint(long userId, Role actual, Role required) {
        return "Accesso negato: l'utente " + userId + " ha ruolo " + actual + ", richiesto " + required + ".";
    }

    /** Tentativo di accesso a risorsa host di un altro host. */
    public static String forbiddenHostResource(String resourceKind, long pathHostId) {
        return "Accesso negato: non puoi accedere a " + resourceKind + " per l'host " + pathHostId + ".";
    }

    /** Utente sospeso prova a usare un endpoint. */
    public static String forbiddenUserDisabled(long userId) {
        return "Accesso negato: l'utente " + userId + " è disattivato.";
    }

    /** Host non ancora approvato prova a usare endpoint riservati agli host attivi. */
    public static String forbiddenHostPending(long userId) {
        return "Accesso negato: l'host " + userId + " è in attesa di approvazione.";
    }
}
