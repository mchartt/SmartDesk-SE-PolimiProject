package it.polimi.smartdesk_backend.exception;

import lombok.NoArgsConstructor;
/** Conflitto stato risorsa (optimistic lock, univocità) → HTTP 409. */
@NoArgsConstructor
public class ConflictException extends RuntimeException {

    /**
     * @param message spiegazione per l'utente o il client API
     */
    public ConflictException(String message) {
        super(message);
    }
}

