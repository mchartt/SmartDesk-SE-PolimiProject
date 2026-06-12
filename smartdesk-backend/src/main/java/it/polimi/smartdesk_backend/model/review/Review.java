package it.polimi.smartdesk_backend.model.review;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Recensione post-prenotazione: rating, commento. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewID;

    private Long workerID;
    private Long hostID;
    private Long spaceID;
    private Long bookingID;
    /** Il voto dato al desk/ufficio (solitamente da 1 a 5). */
    private int rating;
    /** Commento del worker sull'esperienza. */
    private String comment;
    private LocalDate createdAt;

    /** Indica se l'host ha letto la recensione. */
    @Column(nullable = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean seenByHost;



    /** Controlla se l'host ha già visto la recensione (tratta null come false). */
    public boolean isSeenByHost() {
        return Boolean.TRUE.equals(seenByHost);
    }

    /** Segna la recensione come vista dall'host. */
    public void setSeenByHost(Boolean seenByHost) {
        this.seenByHost = seenByHost;
    }

}

