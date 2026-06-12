package it.polimi.smartdesk_backend.dto.notification;

/** Payload SSE quando una notifica cambia stato lettura (es. segna come letta su altro tab). */
public record NotificationUpdatedEventDTO(long notificationID, boolean read) {
}
