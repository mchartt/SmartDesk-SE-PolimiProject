package it.polimi.smartdesk_backend.service.notification;
import it.polimi.smartdesk_backend.mapper.NotificationMapper;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;

/** Operazioni inbox lato destinatario: lista recenti, conteggio unread, mark read, purge storico letto. */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    static final int INBOX_LIMIT = 200;

    private final NotificationRepository notificationRepository;
    private final NotificationRealtimeBroadcaster realtimeBroadcaster;

    /** Ultime notifiche del destinatario, dalla più recente (max {@link #INBOX_LIMIT}). */
    @Transactional(readOnly = true)
    public List<NotificationDTO> listForRecipient(Long recipientUserId) {
        return notificationRepository.findTop200ByRecipientIDOrderByCreatedAtDesc(recipientUserId).stream()
                .map(NotificationMapper::fromNotification)
                .toList();
    }

    /** Segna read=true solo se {@code notificationId} appartiene a {@code requesterId}; altrimenti {@code NotFoundException} (non {@code Forbidden}) per non confermare l'esistenza dell'ID a un attaccante. */
    @Transactional
    public void markAsRead(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.notificationNotFound(notificationId)));
        if (!requesterId.equals(notification.getRecipientID())) {
            throw new NotFoundException(ResourceMessage.notificationNotFound(notificationId));
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        realtimeBroadcaster.publishMarkedRead(notification);
    }

    /** Conteggio semplice {@code read=false} per badge UI. */
    @Transactional(readOnly = true)
    public long unreadCount(Long requesterId) {
        return notificationRepository.countByRecipientIDAndReadFalse(requesterId);
    }

    /** Bulk update: ritorna quante righe ha toccato il repository (utile per metriche/test); il controller oggi ignora il valore. */
    @Transactional
    public int markAllAsRead(Long requesterId) {
        int updated = notificationRepository.markAllAsRead(requesterId);
        realtimeBroadcaster.publishAllMarkedRead(requesterId);
        return updated;
    }

    /** DELETE logico-fisico delle notifiche già lette: torna il numero di righe cancellate; le unread restano intatte. */
    @Transactional
    public int clearReadHistory(Long requesterId) {
        return notificationRepository.deleteReadHistory(requesterId);
    }
}

