package it.polimi.smartdesk_backend.model.user;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Host: anagrafica struttura, partita IVA e flag di approvazione piattaforma. */
@Entity
@DiscriminatorValue("HOST")
@Getter
@Setter
@NoArgsConstructor
public class Host extends User {

    /** Nome dell'edificio o azienda. */
    private String nameStructure;
    /** Una descrizione dell'host o della sua attività. */
    private String description;
    /** La Partita IVA dell'host. */
    private String vatNumber;
    /** Se true, l'host è stato verificato e può operare sulla piattaforma. */
    private boolean approved;

}

