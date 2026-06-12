package it.polimi.smartdesk_backend.dto.admin;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/** Voce di audit log piattaforma (azione, ruolo attore, gravità, IP). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDTO {

    private Long logID;
    /** Ruolo dell'attore al momento dell'azione. */
    private String actorRole;
    private String action;
    private LocalDateTime timestamp;
    /** Gravità evento (stringa dominio). */
    private String severity;
    /** Indirizzo IP client se noto. */
    private String ipAddress;
}

