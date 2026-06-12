package it.polimi.smartdesk_backend.dto.notification;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Notifica da mandare al frontend senza portarsi dietro il modello del database. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long notificationID;
    private String message;
    private String kind;
    private String actorName;
    private String actorSurname;
    private String actorEmail;
    private Integer actorRating;
    private boolean read;
    private LocalDateTime createdAt;
}

