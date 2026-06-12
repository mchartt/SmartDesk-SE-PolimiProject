package it.polimi.smartdesk_backend.model.user;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Utente worker: prenotazioni, recensioni e ticket; campi opzionali {@code bio} e {@code company}. */
@Entity
@DiscriminatorValue("WORKER")
@Getter
@Setter
@NoArgsConstructor
public class Worker extends User {

    /** Biografia opzionale del worker. */
    private String bio;
    /** L'azienda del worker. */
    private String company;
}
