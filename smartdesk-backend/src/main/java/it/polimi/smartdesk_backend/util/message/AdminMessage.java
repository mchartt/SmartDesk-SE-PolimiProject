package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Ban, cambio password e violazioni policy lato pannello admin. */
@RequiredArgsConstructor
public enum AdminMessage {
    ADMIN_CANNOT_BAN_SELF("Un amministratore non può bannare se stesso."),
    CURRENT_PASSWORD_WRONG("La password attuale non è corretta."),
    NEW_PASSWORD_POLICY_VIOLATION("La nuova password deve avere almeno 8 caratteri e contenere un numero e un simbolo.");

    private final String text;

    public String text() {
        return text;
    }
}
