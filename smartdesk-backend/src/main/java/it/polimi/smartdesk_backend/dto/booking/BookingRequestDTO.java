package it.polimi.smartdesk_backend.dto.booking;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;

/** Body POST /book: desk e fascia oraria; accetta {@code end} o {@code endTime} in JSON. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    /** Postazione da prenotare. */
    @NotNull(message = "Serve l'identificativo della postazione (desk).")
    private Long deskID;

    @NotNull(message = "L'orario di inizio è obbligatorio.")
    private LocalDateTime startTime;

    /** Fine intervallo; in JSON è accettato anche il nome {@code endTime}. */
    @NotNull(message = "L'orario di fine è obbligatorio (campo end o endTime in JSON).")
    @JsonAlias({ "endTime" })
    private LocalDateTime end;

    /** Note opzionali del worker. */
    private String notes;

    /** Alias di {@link #end} per client che inviano {@code endTime}. */
    public LocalDateTime getEndTime() {
        return end;
    }

    /** Scrive su {@link #end} accettando il nome {@code endTime}. */
    public void setEndTime(LocalDateTime endTime) {
        this.end = endTime;
    }

}

