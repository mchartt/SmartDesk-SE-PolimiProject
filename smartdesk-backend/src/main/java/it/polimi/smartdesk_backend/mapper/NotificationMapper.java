package it.polimi.smartdesk_backend.mapper;

import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.model.notification.Notification;

/** Converte la notifica del database nell'oggetto da mandare al frontend. */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    /** Copia i campi dal database all'oggetto da mandare in risposta. */
    public static NotificationDTO fromNotification(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationID(notification.getNotificationID());
        dto.setMessage(notification.getMessage());
        dto.setKind(notification.getKind());
        dto.setActorName(notification.getActorName());
        dto.setActorSurname(notification.getActorSurname());
        dto.setActorEmail(notification.getActorEmail());
        dto.setActorRating(notification.getActorRating());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
