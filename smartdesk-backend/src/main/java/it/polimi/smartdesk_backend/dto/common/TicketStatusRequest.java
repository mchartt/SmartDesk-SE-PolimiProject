package it.polimi.smartdesk_backend.dto.common;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Aggiornamento stato ticket da tecnico/admin: stato obbligatorio, nota e severità opzionali. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TicketStatusRequest {
    @NotBlank(message = "Lo stato del ticket è obbligatorio.")
    String status;
    /** Note operative del tecnico durante l'intervento. */
    String note;
    /** Descrizione della risoluzione al momento della chiusura (separata dalla nota tecnico). */
    String resolution;
    String severity;
    /** Data/ora stimata di risoluzione (opzionale, aggiornabile senza cambio stato). */
    LocalDateTime estimatedResolutionAt;
}

