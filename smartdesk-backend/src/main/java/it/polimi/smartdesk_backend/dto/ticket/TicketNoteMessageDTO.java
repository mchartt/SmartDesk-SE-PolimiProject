package it.polimi.smartdesk_backend.dto.ticket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Messaggio nello storico note tecnico esposto al worker (chat-style). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketNoteMessageDTO {

    private String body;
    private LocalDateTime createdAt;
    /** Etichetta autore chat, es. {@code Mario Rossi · Utente}. */
    private String authorLabel;
}
