package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Regole temporali e di concorrenza sulle prenotazioni: finestre, overlap, manutenzione, riprogrammazione. */
@RequiredArgsConstructor
public enum BookingMessage {
    END_AFTER_START("L'orario di fine deve essere successivo all'inizio."),
    START_END_SAME_DAY("Inizio e fine devono cadere nello stesso giorno."),
    START_IN_FUTURE("L'orario di inizio deve essere nel futuro."),
    END_IN_FUTURE("L'orario di fine deve essere nel futuro."),
    BOOKING_DAY_WINDOW("La data della prenotazione deve essere nei prossimi 7 giorni."),
    WORKER_SLOT_OVERLAP("Hai già una prenotazione che si sovrappone a questa fascia."),
    DESK_UNDER_MAINTENANCE("La postazione è in manutenzione."),
    DESK_ALREADY_BOOKED("La postazione risulta già occupata nella fascia oraria selezionata."),
    BOOKING_CONCURRENT_RETRY(
            "Non siamo riusciti a confermare la prenotazione per un conflitto temporaneo. Riprova tra qualche secondo."),
    BOOKING_STATUS_UNKNOWN("Stato prenotazione non riconosciuto: non è possibile riprogrammarla."),
    BOOKING_CANNOT_RESCHEDULE("Questa prenotazione non può essere riprogrammata."),
    BOOKING_VERSION_STALE("La prenotazione è stata aggiornata nel frattempo. Ricarica e riprova."),
    RESCHEDULE_END_AFTER_START("La nuova fine deve essere successiva al nuovo inizio."),
    RESCHEDULE_START_FUTURE("Il nuovo orario di inizio deve essere nel futuro."),
    START_TIME_REQUIRED("L'orario di inizio è obbligatorio."),
    END_TIME_REQUIRED("L'orario di fine è obbligatorio."),
    START_TIME_TOO_CLOSE("L'orario di inizio è troppo vicino; lascia almeno 30 minuti di margine."),
    BOOKING_CODE_ALLOCATION_FAILED("Impossibile assegnare un codice prenotazione univoco."),
    BOOKING_CODE_CONFLICT("Il codice prenotazione generato è già presente. Riprova l'operazione."),
    BOOKING_CANNOT_CANCEL_IN_PROGRESS(
            "La prenotazione è già iniziata: non puoi annullarla. Usa «Lascia postazione» per terminare la sessione."),
    BOOKING_NOT_IN_PROGRESS("La prenotazione non è in corso: non è possibile lasciare la postazione."),
    BOOKING_LEAVE_END_AFTER_START("L'orario di interruzione deve essere successivo all'inizio della prenotazione.");

    private final String text;

    public String text() {
        return text;
    }
}
