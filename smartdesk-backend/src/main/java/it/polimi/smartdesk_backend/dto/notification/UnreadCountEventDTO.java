package it.polimi.smartdesk_backend.dto.notification;

/** Payload SSE inviato al client quando cambia il numero di notifiche non lette. */
public record UnreadCountEventDTO(long unreadCount) {
}
