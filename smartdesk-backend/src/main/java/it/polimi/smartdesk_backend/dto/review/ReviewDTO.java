package it.polimi.smartdesk_backend.dto.review;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload creazione o aggiornamento recensione: voto 1–5 e commento con vincoli di validazione. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    /** Prenotazione a cui è associata la recensione. */
    @NotNull(message = "L'identificativo della prenotazione è obbligatorio.")
    private Long bookingID;

    /** Host destinazione (uso contestuale lato API). */
    private Long hostID;

    @Min(value = 1, message = "Il voto minimo è 1.")
    @Max(value = 5, message = "Il voto massimo è 5.")
    private int rating;

    /** Spazio recensito (se diverso dalla sola prenotazione). */
    private Long spaceID;

    @NotBlank(message = "Il commento è obbligatorio.")
    @Size(min = 50, max = 250, message = "Il commento deve avere tra 50 e 250 caratteri.")
    private String comment;

}

