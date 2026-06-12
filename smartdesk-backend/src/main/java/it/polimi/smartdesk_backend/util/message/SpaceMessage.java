package it.polimi.smartdesk_backend.util.message;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

/** Validazione catalogo sedi: stanze, desk, preset servizi, chiusure, orari e assegnazioni tecnici. */
@RequiredArgsConstructor
public enum SpaceMessage {
    ROOM_NAME_REQUIRED("Il nome della stanza è obbligatorio."),
    ROOM_CODE_IN_USE("Questo codice stanza è già usato in questo spazio."),
    ROOM_DELETE_HAS_DESKS("Non puoi eliminare una stanza che contiene ancora postazioni."),
    PRESET_LABEL_REQUIRED("L'etichetta del preset è obbligatoria."),
    PRESET_HINT_REQUIRED("La descrizione breve del preset è obbligatoria."),
    SPACE_ID_REQUIRED("Lo spazio è obbligatorio (spaceID)."),
    ROOM_ID_REQUIRED("La stanza è obbligatoria (roomID)."),
    DESK_CODE_IN_USE("Questo codice postazione è già usato in questo spazio."),
    PRESET_MIN_ONE_AMENITY("Il preset deve contenere almeno un servizio."),
    DESK_CODE_FORMAT("Il codice postazione deve iniziare con una lettera e contenere solo lettere e numeri (max 16 caratteri)."),
    ROOM_CODE_FORMAT("Il codice stanza deve avere 2–10 lettere maiuscole o cifre."),
    PRESET_DUPLICATE_LABEL("Esiste già un preset con la stessa etichetta in questo spazio."),
    PRESET_DUPLICATE_AMENITIES("Esiste già un preset con la stessa lista di servizi in questo spazio."),
    CLOSURE_DATES_NON_EMPTY("Indica almeno un giorno di chiusura."),
    CLOSURE_REASON_REQUIRED("Il motivo della chiusura è obbligatorio."),
    HOST_NOT_APPROVED("Account host non ancora approvato."),
    OPENING_HOURS_INVALID_PAYLOAD("Formato orari di apertura non valido."),
    OFFICE_CODE_ALLOCATION_FAILED("Impossibile assegnare un codice sede univoco."),
    OFFICE_CODE_CONFLICT("Il codice sede generato è già presente. Riprova l'operazione."),
    SPACE_CLOSED_ON_SELECTED_DAY("La sede è chiusa nel giorno selezionato."),
    SPACE_CLOSED_ON_SELECTED_DATE("La sede è chiusa nella data selezionata."),
    BOOKING_OUTSIDE_OPENING_HOURS("La prenotazione è fuori dall'orario di apertura della sede."),
    TECHNICIAN_ALREADY_ASSIGNED("Questo tecnico è già assegnato a questo spazio.");

    private final String text;

    public String text() {
        return text;
    }

    public static String closurePastDay(LocalDate day) {
        return "Non puoi chiudere giorni già passati: " + day;
    }

    public static String closureAlreadyExists(LocalDate day) {
        return "Esiste già una chiusura per il giorno " + day;
    }

    /** Chiave giorno non riconosciuta nel JSON degli orari (es. errore di battuta). */
    public static String openingHoursUnknownDay(String key) {
        return "Giorno non valido negli orari: " + key;
    }

    /** La sede è aperta ma mancano apertura o chiusura. */
    public static String openingHoursOpenCloseRequired(String day) {
        return "Orari per " + day + ": con la sede aperta servono sia apertura sia chiusura.";
    }

    public static String openingHoursUse24h(String day) {
        return "Orari per " + day + ": usa HH:mm (24h).";
    }

    public static String openingHoursCloseAfterOpen(String day) {
        return "Orari per " + day + ": la chiusura deve essere dopo l'apertura.";
    }

    /** L'host non è il proprietario dello spazio (anti-IDOR, esposto come 404). */
    public static String hostNotOwnerOfSpace(long spaceId) {
        return "L'host non risulta titolare dello spazio (id=" + spaceId + ").";
    }

    /** La postazione non è collegata ad alcun spazio (dato incoerente). */
    public static String deskWithoutSpace(long deskId) {
        return "La postazione " + deskId + " non risulta associata a uno spazio.";
    }
}
