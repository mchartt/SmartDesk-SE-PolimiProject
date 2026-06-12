package it.polimi.smartdesk_backend.model.booking;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** Prenotazione worker su desk: intervallo {@code startTime}/{@code endTime}, {@code bookingCode} univoco, stato e {@code version}. */
@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    /** Quanti giorni ha il worker per lasciare una recensione dopo che la prenotazione è finita. */
    public static final int REVIEW_ELIGIBILITY_DAYS = 14;

    /** ID interno della prenotazione. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking")
    private Long bookingID;

    /** Codice pubblico a 6 caratteri (es. AB1234) da mostrare al worker. */
    @Column(name = "booking_code", length = 6, unique = true)
    private String bookingCode;

    private Long workerID;
    private Long deskID;
    private LocalDate bookedDay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status = BookingStatus.PENDING.name();

    /** Versione riga per evitare riscritture concorrenti sulla stessa prenotazione. */
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    private Long version = 0L;

    /** Normalizza eventuali righe legacy senza versione. */
    @PostLoad
    @SuppressWarnings("unused")
    private void ensureVersionForOptimisticLock() {
        if (version == null) {
            version = 0L;
        }
    }

    /** Prima del primo salvataggio la versione deve partire da 0. */
    @PrePersist
    @SuppressWarnings("unused")
    private void prePersist() {
        if (version == null) {
            version = 0L;
        }
    }

    /** Indica se la prenotazione è conclusa (orario passato e stato confermato). */
    public boolean isCompleted(LocalDateTime now) {
        return endTime != null && endTime.isBefore(now) && BookingStatus.CONFIRMED.name().equals(status);
    }

    /** Controlla se il worker può ancora scrivere una recensione. Deve essere finita da meno di 14 giorni. */
    public boolean isEligibleForReview(LocalDateTime now) {
        if (!BookingStatus.CONFIRMED.name().equals(status) || endTime == null) {
            return false;
        }
        if (!endTime.isBefore(now)) {
            return false;
        }
        LocalDateTime cutoff = now.minusDays(REVIEW_ELIGIBILITY_DAYS);
        return endTime.isAfter(cutoff);
    }

    /** Annulla la prenotazione. */
    public void cancel() {
        status = BookingStatus.CANCELLED.name();
    }

    /** Conferma la prenotazione. */
    public void confirm() {
        status = BookingStatus.CONFIRMED.name();
    }
}

