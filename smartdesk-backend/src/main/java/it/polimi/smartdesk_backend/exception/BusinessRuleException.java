package it.polimi.smartdesk_backend.exception;

import lombok.NoArgsConstructor;
/** Violazione regola dominio → HTTP 400 {@code BUSINESS_RULE_VIOLATION} via {@link RestExceptionHandler}. */
@NoArgsConstructor
public class BusinessRuleException extends RuntimeException {

    /**
     * @param message testo mostrato al client (spesso da enum in {@code util.message})
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}

