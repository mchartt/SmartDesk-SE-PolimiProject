package it.polimi.smartdesk_backend.dto.booking;

import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Riprogrammazione di una prenotazione esistente (nuovo intervallo nel futuro). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleBookingDTO {

    /** Prenotazione da spostare. */
    @NotNull(message = "L'identificativo della prenotazione è obbligatorio.")
    private Long bookingId;

    /** Versione letta dal client: blocca riprogrammazioni basate su dati vecchi. */
    @NotNull(message = "version è obbligatorio.")
    private Long version;

    /** Nuovo inizio intervallo (deve essere nel futuro). */
    @NotNull(message = "Il nuovo orario di inizio è obbligatorio.")
    private LocalDateTime newStart;

    /** Nuova fine intervallo (deve essere nel futuro e dopo l'inizio). */
    @NotNull(message = "Il nuovo orario di fine è obbligatorio.")
    private LocalDateTime newEnd;

    /** Verifica che {@code newEnd} sia successivo a {@code newStart} quando entrambi sono valorizzati. */
    @AssertTrue(message = "La nuova fine deve essere successiva al nuovo inizio.")
    public boolean isNewEndAfterNewStart() {
        return newStart == null || newEnd == null || newEnd.isAfter(newStart);
    }
}

