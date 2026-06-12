package it.polimi.smartdesk_backend.model.booking;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Una riga per worker+desk+giorno; notified=true dopo la prima notifica "si è liberato". */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "workerID", "deskID", "bookedDay" })
})
@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workerID;
    private Long deskID;
    private LocalDate bookedDay;
    /** Inizio slot desiderato. Se impostato con {@code desiredEndTime}, notifica solo per slot compatibili. */
    private LocalDateTime desiredStartTime;
    /** Fine slot desiderato. */
    private LocalDateTime desiredEndTime;
    private boolean notified;
    private LocalDateTime createdAt;

    /** Valorizza {@code createdAt} alla creazione se non impostato. */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

