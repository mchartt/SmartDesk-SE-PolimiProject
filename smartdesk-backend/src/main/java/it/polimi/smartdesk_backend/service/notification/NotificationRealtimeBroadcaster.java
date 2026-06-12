package it.polimi.smartdesk_backend.service.notification;
import it.polimi.smartdesk_backend.mapper.NotificationMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.dto.notification.NotificationUpdatedEventDTO;
import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;

/** Dopo ogni modifica alle notifiche di un utente, invia eventi SSE (conteggio, creazione, aggiornamento). */
@Service
@RequiredArgsConstructor
public class NotificationRealtimeBroadcaster {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamHub notificationStreamHub;

    /** Ricalcola il conteggio unread e lo propaga via SSE al destinatario; nessuna operazione se {@code recipientId} è nullo. */
    @Transactional(readOnly = true)
    public void publishForRecipient(Long recipientId) {
        if (recipientId == null) {
            return;
        }
        long count = notificationRepository.countByRecipientIDAndReadFalse(recipientId);
        notificationStreamHub.publishUnreadCount(recipientId, count);
    }

    /** Manda la notifica sul momento al client e gli aggiorna il contatore di quelle non lette. */
    public void publishCreated(Notification notification) {
        if (notification == null || notification.getRecipientID() == null) {
            return;
        }
        Long recipientId = notification.getRecipientID();
        notificationStreamHub.publishNotificationCreated(
                recipientId, NotificationMapper.fromNotification(notification));
        publishForRecipient(recipientId);
    }

    /**
     * Propaga via SSE un aggiornamento dopo mark-as-read singolo e ricalcola il badge unread.
     *
     * @param notification notifica aggiornata
     */
    public void publishMarkedRead(Notification notification) {
        if (notification == null || notification.getRecipientID() == null) {
            return;
        }
        Long recipientId = notification.getRecipientID();
        notificationStreamHub.publishNotificationUpdated(
                recipientId,
                new NotificationUpdatedEventDTO(notification.getNotificationID(), notification.isRead()));
        publishForRecipient(recipientId);
    }

    /**
     * Propaga via SSE l'evento mark-all-read e ricalcola il badge unread.
     *
     * @param recipientId ID dell'utente che ha eseguito mark all read
     */
    public void publishAllMarkedRead(Long recipientId) {
        if (recipientId == null) {
            return;
        }
        notificationStreamHub.publishAllMarkedRead(recipientId);
        publishForRecipient(recipientId);
    }
}
