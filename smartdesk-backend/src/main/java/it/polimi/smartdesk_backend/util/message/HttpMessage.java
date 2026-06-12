package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Errori “di trasporto”: JSON malformato, metodo non supportato, parametri/header mancanti, conflitti generici. */
@RequiredArgsConstructor
public enum HttpMessage {
    MALFORMED_JSON_BODY("Corpo della richiesta JSON non valido."),
    DATA_INTEGRITY_USER_MESSAGE("L'operazione è in conflitto con i vincoli sui dati."),
    INTERNAL_ERROR_USER("Errore interno del server."),
    SUPPORTED_METHODS_LABEL("Metodi supportati"),
    DATA_CONFLICT("I dati sono stati aggiornati nel frattempo. Ricarica la pagina e riprova.");

    private final String text;

    public String text() {
        return text;
    }

    /** Metodo HTTP non supportato dall'endpoint (405). */
    public static String httpMethodNotSupported(String method) {
        return "Metodo HTTP non supportato: " + method + ".";
    }

    /** Tipo atteso per un parametro quando arriva un valore di tipo errato. */
    public static String typeMismatchExpectedType(String simpleTypeName) {
        String label = simpleTypeName == null || simpleTypeName.isBlank() ? "sconosciuto" : simpleTypeName;
        return "Tipo atteso: " + label + ".";
    }

    /** Valore non valido per query parameter (400). */
    public static String invalidQueryParameter(String name) {
        return "Valore non valido per il parametro: " + name;
    }

    public static String missingHeader(String headerName) {
        return "Header obbligatorio mancante: " + headerName;
    }

    public static String missingParameter(String paramName) {
        return "Parametro obbligatorio mancante: " + paramName;
    }

    /** Codice univoco non allocabile dopo più tentativi (es. bookingCode, officeCode). */
    public static String uniqueCodeAllocationFailed(String context, int maxAttempts) {
        return "Impossibile assegnare codice univoco per " + context + " dopo " + maxAttempts + " tentativi";
    }
}
