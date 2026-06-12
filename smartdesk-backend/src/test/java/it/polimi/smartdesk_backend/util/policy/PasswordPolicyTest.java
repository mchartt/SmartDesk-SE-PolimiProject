package it.polimi.smartdesk_backend.util.policy;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Regola password forte: lunghezza minima e presenza di tipi di carattere richiesti. */
@FieldDefaults(level = AccessLevel.PRIVATE)
class PasswordPolicyTest {

    @Test
    void passwordOkEightCharacters() {
        assertTrue(PasswordPolicy.isStrong("Secret1!"));
    }

    @Test
    void passwordTooShort() {
        assertFalse(PasswordPolicy.isStrong("S1!abcd"));
    }

    @Test
    void passwordWithoutDigit() {
        assertFalse(PasswordPolicy.isStrong("Secret!!secret"));
    }

    @Test
    void passwordWithoutSymbol() {
        assertFalse(PasswordPolicy.isStrong("Secret11secret"));
    }
}
