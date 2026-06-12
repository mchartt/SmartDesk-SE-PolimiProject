package it.polimi.smartdesk_backend.dto.ticket;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload worker per apertura ticket: {@code bookingID} o {@code deskCode}; le costanti messaggio sono condivise col service. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    public static final int DESCRIPTION_MAX_LENGTH = 1000;
    public static final int TITLE_MAX_LENGTH = 30;

    public static final String TITLE_REQUIRED = "Il titolo è obbligatorio.";
    public static final String TITLE_TOO_LONG = "Il titolo non può superare {max} caratteri.";
    public static final String DESCRIPTION_REQUIRED = "La descrizione è obbligatoria.";
    public static final String DESCRIPTION_TOO_LONG = "La descrizione non può superare {max} caratteri.";
    public static final String BOOKING_OR_DESK_REQUIRED = "Indicare bookingID o deskCode.";

    /** Identificativo prenotazione collegata; preferito se il codice postazione non è univoco tra spazi. */
    private Long bookingID;

    /** Codice postazione se non si usa {@code bookingID}. */
    private String deskCode;

    @NotBlank(message = TITLE_REQUIRED)
    @Size(max = TITLE_MAX_LENGTH, message = TITLE_TOO_LONG)
    private String title;

    @NotBlank(message = DESCRIPTION_REQUIRED)
    @Size(max = DESCRIPTION_MAX_LENGTH, message = DESCRIPTION_TOO_LONG)
    private String description;

    /** Gravità segnalata (es. stringa lato dominio/UI). */
    private String severity;

    /** Controlla che ci sia almeno un modo per risalire alla postazione: o l'ID della prenotazione o il codice del desk. */
    @AssertTrue(message = BOOKING_OR_DESK_REQUIRED)
    public boolean isAtLeastOneIdentifierPresent() {
        if (bookingID != null) {
            return true;
        }
        return deskCode != null && !deskCode.isBlank();
    }

}

