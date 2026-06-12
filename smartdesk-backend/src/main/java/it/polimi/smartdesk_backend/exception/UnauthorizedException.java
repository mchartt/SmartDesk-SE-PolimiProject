package it.polimi.smartdesk_backend.exception;

import lombok.NoArgsConstructor;

/** Credenziali/token non validi → HTTP 401 (filtro JWT o {@link it.polimi.smartdesk_backend.service.security.AuthService}). */
@NoArgsConstructor
public class UnauthorizedException extends RuntimeException {

    /**
     * @param message testo coerente con {@link it.polimi.smartdesk_backend.util.message.AuthMessage}
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}

