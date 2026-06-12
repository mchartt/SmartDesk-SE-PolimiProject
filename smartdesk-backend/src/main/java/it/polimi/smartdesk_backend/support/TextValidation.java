package it.polimi.smartdesk_backend.support;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Piccolo helper per stringhe obbligatorie: trim e {@link BusinessRuleException} se vuote. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextValidation {

    /**
     * @param emptyMessage messaggio business se dopo il trim non resta nulla
     * @return testo trimmed non vuoto
     * @throws BusinessRuleException input assente o solo spazi
     */
    public static String requireTrimmed(String raw, String emptyMessage) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessRuleException(emptyMessage);
        }
        return trimmed;
    }
}

