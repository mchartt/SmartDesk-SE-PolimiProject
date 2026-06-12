package it.polimi.smartdesk_backend.exception;

import lombok.NoArgsConstructor;
/** Autenticato ma non autorizzato → HTTP 403. */
@NoArgsConstructor
public class ForbiddenException extends RuntimeException {

    /**
     * @param message motivo del rifiuto (ownership, ruolo, host pending, …)
     */
    public ForbiddenException(String message) {
        super(message);
    }
}

