package it.polimi.smartdesk_backend.model.space;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fascia oraria per giorno della settimana, legata a {@link Space}. */
@Entity
@Table(name = "opening_hours")
@Getter
@Setter
@NoArgsConstructor
public class OpeningHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    /** Giorno della settimana (es. MONDAY, TUESDAY...). */
    private String dayOfWeek;

    /** Orario di apertura in formato HH:mm (es. 08:30). */
    private String openTime;

    /** Orario di chiusura in formato HH:mm (es. 18:30). */
    private String closeTime;

    /** Se true, lo spazio è chiuso per l'intero giorno. */
    private boolean closed = false;

    /** Costruttore compatto con i campi obbligatori per una fascia oraria. */
    public OpeningHours(Space space, String dayOfWeek, String openTime, String closeTime, boolean closed) {
        this.space = space;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.closed = closed;
    }
}

