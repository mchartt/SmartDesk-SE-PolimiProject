package it.polimi.smartdesk_backend.model.space;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/** Calendario chiusure: un giorno bloccato per uno space (coppia space+data unica). */
@Entity
@Table(
        name = "space_closure",
        uniqueConstraints = @UniqueConstraint(name = "uk_space_closure_date", columnNames = { "space_id", "closed_date" }))
@Getter
@Setter
@NoArgsConstructor
public class SpaceClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "closed_date", nullable = false)
    private LocalDate closedDate;

    /** Motivazione della chiusura, inclusa nelle notifiche di cancellazione. */
    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;
}

