package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Apertura e gestione ticket assistenza: legame con prenotazione odierna, codici postazione, stati tecnico. */
@RequiredArgsConstructor
public enum TicketMessage {
    TICKET_BOOKING_NOT_FOUND("Prenotazione non trovata."),
    TICKET_ONLY_TODAY_BOOKING("Puoi aprire una segnalazione solo per una postazione prenotata oggi."),
    TICKET_CANCELLED_BOOKING("Non puoi segnalare un problema su una prenotazione annullata."),
    TICKET_DESK_NOT_FOUND_GENERIC("Postazione non trovata."),
    TICKET_DESK_CODE_MISMATCH("Il codice postazione non corrisponde alla prenotazione selezionata."),
    TICKET_AMBIGUOUS_DESK_CODE("Più postazioni corrispondono a questo codice oggi: invia bookingID per identificarla."),
    TICKET_CODE_ALLOCATION_FAILED("Impossibile generare un codice ticket univoco."),
    TICKET_STATUS_REQUIRED("Lo stato è obbligatorio."),
    TICKET_RESOLUTION_REQUIRED("La risoluzione è obbligatoria per chiudere la segnalazione."),
    TECHNICIAN_DELETE_ACTIVE_TICKETS("Impossibile eliminare il tecnico: ticket ancora aperti o in lavorazione."),
    TICKET_DELETE_IN_PROGRESS("Il ticket è in carico al tecnico e non può essere eliminato."),
    TICKET_DELETE_ALREADY_RESOLVED("Il ticket è risolto e non può più essere eliminato."),
    TICKET_ASSIGN_NO_SPACE(
            "Impossibile assegnare un tecnico: lo spazio associato alla postazione non è stato trovato."),
    TICKET_ASSIGN_ONLY_WHEN_OPEN("Puoi assegnare un tecnico solo quando il ticket è ancora aperto."),
    TICKET_VERIFY_NOT_ASSIGNED("Non puoi risolvere un ticket non ancora preso in carico."),
    TICKET_APPROVE_NOT_RESOLVED("Non puoi approvare un ticket non ancora risolto."),
    TICKET_REJECT_NOT_RESOLVED("Non puoi respingere un ticket non ancora risolto."),
    TICKET_IN_PROGRESS_CANNOT_APPROVE("Il ticket è ancora in lavorazione e non può essere approvato."),
    TICKET_ALREADY_IN_PROGRESS("Il ticket è già in lavorazione."),
    TICKET_ALREADY_VERIFYING("Il ticket è già in fase di verifica."),
    TICKET_ALREADY_RESOLVED_APPROVED("Il ticket è già stato risolto e approvato."),
    TICKET_ALREADY_RESOLVED("Il ticket è già stato risolto."),
    TICKET_REJECT_ALREADY_APPROVED("Non puoi respingere un ticket già approvato."),
    TICKET_CLOSED("Il ticket è chiuso."),
    TICKET_REJECT_CLOSED("Non puoi respingere un ticket chiuso."),
    TICKET_ALREADY_CLOSED_OR_RESOLVED("Il ticket è già chiuso o risolto."),
    TICKET_COMMENT_ROLE_FORBIDDEN("Ruolo non autorizzato al commento."),
    TICKET_COMMENT_ON_CLOSED("Non puoi aggiungere commenti su un ticket già risolto o chiuso.");

    private final String text;

    public String text() {
        return text;
    }

    /** Postazione non trovata per codice (es. errore battitura). */
    public static String deskNotFoundByCode(String code) {
        return "Postazione con codice " + code + " non trovata.";
    }

    /** Titolo troppo lungo rispetto al limite configurato. */
    public static String ticketTitleTooLong(int maxLen) {
        return "Il titolo non può superare " + maxLen + " caratteri.";
    }

    /** Commento troppo lungo rispetto al limite configurato. */
    public static String ticketCommentTooLong(int maxLen) {
        return "Il commento non può superare " + maxLen + " caratteri.";
    }
}
