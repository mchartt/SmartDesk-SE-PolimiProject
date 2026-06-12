package it.polimi.smartdesk_backend.exception;

import lombok.NoArgsConstructor;
/** Risorsa assente o mascherata → HTTP 404; messaggio spesso generico per anti-enumeration ID. */
@NoArgsConstructor
public class NotFoundException extends RuntimeException {

    /**
     * @param message descrizione per il client
     */
    public NotFoundException(String message) {
        super(message);
    }
}

