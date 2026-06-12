package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Vincoli sulle recensioni lato worker/host: proprietà, reazioni, lunghezza note, eleggibilità temporale. */
@RequiredArgsConstructor
public enum ReviewMessage {
    REVIEW_BOOKING_ID_REQUIRED("L'identificativo della prenotazione è obbligatorio."),
    REVIEW_BOOKING_NOT_YOURS("La prenotazione non appartiene a questo account."),
    REVIEW_NOT_OWNED_BY_WORKER("Non puoi operare su una recensione che non è tua."),
    REVIEW_HOST_NOTE_TOO_LONG("La nota supera i 200 caratteri."),
    REVIEW_BOOKING_MUST_BE_CONFIRMED("La prenotazione deve essere confermata per poter lasciare una recensione.");

    private final String text;

    public String text() {
        return text;
    }

    /** Finestra temporale scaduta per lasciare una recensione. */
    public static String reviewEligibilityExpired(int days) {
        return "Puoi recensire solo prenotazioni confermate concluse negli ultimi " + days + " giorni.";
    }

    public static String reviewAlreadyExists(long bookingId) {
        return "Esiste già una recensione per la prenotazione " + bookingId + ".";
    }

    /** La recensione non appartiene allo spazio del path (anti-IDOR). */
    public static String reviewNotInSpace(long spaceId) {
        return "La recensione non appartiene a questo spazio (id=" + spaceId + ").";
    }
}
