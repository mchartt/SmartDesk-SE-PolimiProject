package it.polimi.smartdesk_backend.dto.auth;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/** Un riassunto dei tuoi dati profilo. Lo usiamo per farti vedere chi sei (nome, mail, ruolo) o per far vedere agli admin chi si è registrato alla piattaforma, senza però mostrare dati sensibili come la password. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    /** Identificativo utente. */
    private Long userID;
    private String name;
    private String surname;
    /** Solo host: descrizione attività mostrata agli admin in approvazione. */
    private String description;
    /** (Solo host) Nome della struttura o società. */
    private String nameStructure;
    private String email;
    private String role;
    /** Stato account (es. stringa dominio). */
    private String status;
    /** Approvazione host/spazio a seconda del contesto. */
    private boolean approved;
    private LocalDateTime registeredAt;
}

