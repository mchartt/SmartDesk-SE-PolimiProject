package it.polimi.smartdesk_backend.dto.ticket;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Ticket espanso per API: anagrafica worker/tecnico, codici desk/spazio e thread note. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {

    private Long ticketID;
    /** Codice leggibile lato UI (es. {@code T7K2}). */
    private String ticketCode;
    /** Titolo breve segnalato dal worker; {@code null} su righe legacy. */
    private String title;
    private String status;
    private Long assignedTechID;
    /** Nome tecnico assegnato (denormalizzato per host). */
    private String assignedTechName;
    private String assignedTechSurname;
    private String resolution;
    private String description;
    private String technicianNote;
    /** Storico note tecnico (ordine cronologico) per UI chat worker. */
    private List<TicketNoteMessageDTO> technicianNoteHistory;
    /** Commenti aggiuntivi del worker dopo l'apertura del ticket. */
    private List<TicketNoteMessageDTO> workerNoteHistory;
    /** Note lasciate dall'host sul ticket. */
    private List<TicketNoteMessageDTO> hostNoteHistory;
    private String deskCode;
    /** ID interno postazione; opzionale se il client usa solo codici. */
    private Long deskID;
    /** Spazio della postazione (filtri UI tecnico). */
    private Long spaceID;
    private String spaceName;
    /** Codice ufficio spazio se disponibile. */
    private String officeCode;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String severity;
    private Long workerID;
    private String workerName;
    private String workerSurname;
    private String workerEmail;
    private LocalDateTime estimatedResolutionAt;

}

