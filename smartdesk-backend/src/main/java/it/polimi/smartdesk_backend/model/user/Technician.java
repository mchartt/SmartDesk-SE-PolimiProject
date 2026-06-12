package it.polimi.smartdesk_backend.model.user;
import it.polimi.smartdesk_backend.model.space.Space;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/** Utente tecnico: {@code creatingHostId}, specializzazione e many-to-many con {@link Space} assegnati. */
@Entity
@DiscriminatorValue("TECHNICIAN")
@Getter
@Setter
@NoArgsConstructor
public class Technician extends User {

    /** L'ID dell'host che ha creato questo account tecnico. */
    @Column(name = "creating_host_id")
    private Long creatingHostId;

    /** In cosa è esperto il tecnico. */
    private String specialization;

    /** Gli uffici dove questo tecnico può intervenire. */
    @ManyToMany
    @JoinTable(
            name = "technician_space",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "space_id"))
    private Set<Space> spaces = new HashSet<>();

    /** Assegna il tecnico a un nuovo ufficio. */
    public void assignSpace(Space space) {
        spaces.add(space);
    }

    /** Rimuove l'assegnazione del tecnico a uno spazio. */
    public void removeSpace(Space space) {
        spaces.remove(space);
    }
}

