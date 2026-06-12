package it.polimi.smartdesk_backend.model.notification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Notifica in-app persistita con {@code kind} opzionale e metadati attore per la UI. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationID;

    private Long recipientID;
    private String message;
    /** Tipo evento (es. HOST_TICKET_OPENED, HOST_REVIEW_LEFT); null per notifiche legacy. */
    private String kind;
    private String actorName;
    private String actorSurname;
    private String actorEmail;
    /** Voto 1–5 per notifiche recensione (es. HOST_REVIEW_LEFT); null per altri tipi. */
    private Integer actorRating;
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
    private LocalDateTime createdAt;

    /** Imposta {@code createdAt} se assente prima del primo salvataggio. */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

