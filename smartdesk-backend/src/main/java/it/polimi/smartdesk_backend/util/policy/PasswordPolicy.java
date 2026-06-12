package it.polimi.smartdesk_backend.util.policy;

import lombok.experimental.UtilityClass;
/** Regole password forte: lunghezza 8–128, almeno una cifra e un carattere non alfanumerico. */
@UtilityClass
public final class PasswordPolicy {

    /**
     * @param password candidata (null → false)
     * @return {@code true} se lunghezza 8–128, almeno una cifra e un carattere non alfanumerico
     */
    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            return false;
        }
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean symbol = password.chars()
                .anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
        return digit && symbol;
    }
}

