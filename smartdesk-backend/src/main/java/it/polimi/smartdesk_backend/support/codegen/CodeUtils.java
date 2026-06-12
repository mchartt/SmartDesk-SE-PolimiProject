package it.polimi.smartdesk_backend.support.codegen;

import java.security.SecureRandom;
import java.util.function.Predicate;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.util.message.HttpMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Generazione codici alfanumerici e allocazione univoca con retry limitato. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeUtils {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera una stringa casuale di lunghezza fissa.
     *
     * @param length numero caratteri da {@link #ALPHANUM}
     * @return stringa maiuscola senza caratteri ambigui (0/O, 1/I)
     */
    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    /**
     * Alloca un codice univoco con retry limitato.
     *
     * @param exists true se il codice è già occupato
     * @param maxAttempts numero massimo di tentativi di generazione
     * @param context etichetta per messaggio errore
     * @return codice di 6 caratteri libero
     * @throws BusinessRuleException se tutti i tentativi falliscono
     */
    public static String allocateUniqueCode(Predicate<String> exists, int maxAttempts, String context) {
        return allocateUniqueCode(exists, maxAttempts, context, "", 6, ALPHANUM);
    }

    /**
     * Alloca un codice univoco con parametri personalizzati.
     *
     * @param exists true se il codice è già occupato
     * @param maxAttempts numero massimo di tentativi
     * @param context etichetta per messaggio errore
     * @param prefix prefisso fisso del codice (es. "T")
     * @param length lunghezza della parte casuale
     * @param alphabet alfabeto da usare per la parte casuale
     * @return codice libero
     * @throws BusinessRuleException se tutti i tentativi falliscono
     */
    public static String allocateUniqueCode(Predicate<String> exists, int maxAttempts, String context, String prefix, int length, String alphabet) {
        for (int i = 1; i <= maxAttempts; i++) {
            StringBuilder sb = new StringBuilder(prefix != null ? prefix : "");
            for (int j = 0; j < length; j++) {
                sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
            }
            String code = sb.toString();
            if (!exists.test(code)) {
                return code;
            }
        }
        throw new BusinessRuleException(HttpMessage.uniqueCodeAllocationFailed(context, maxAttempts));
    }
}

