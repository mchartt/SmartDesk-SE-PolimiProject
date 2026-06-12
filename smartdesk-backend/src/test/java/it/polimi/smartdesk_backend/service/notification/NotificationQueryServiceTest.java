package it.polimi.smartdesk_backend.service.notification;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;

/** Lettura notifiche per utente con controlli di esistenza e ordinamento. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRealtimeBroadcaster realtimeBroadcaster;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    void notificationsForWorker() {
        Notification first = new Notification();
        first.setRecipientID(7L);
        first.setMessage("Primo");
        Notification second = new Notification();
        second.setRecipientID(7L);
        second.setMessage("Secondo");
        when(notificationRepository.findTop200ByRecipientIDOrderByCreatedAtDesc(7L))
            .thenReturn(List.of(first, second));

        List<NotificationDTO> result = notificationQueryService.listForRecipient(7L);
        assertEquals(2, result.size());
        assertEquals("Primo", result.get(0).getMessage());
    }

    @Test
    void emptyInboxEmptyList() {
        when(notificationRepository.findTop200ByRecipientIDOrderByCreatedAtDesc(88L))
            .thenReturn(List.of());

        List<NotificationDTO> result = notificationQueryService.listForRecipient(88L);

        assertTrue(result.isEmpty());
    }

    @Test
    void timelineUsesSameOrderAsRepository() {
        Notification newest = new Notification();
        newest.setRecipientID(12L);
        newest.setMessage("Più recente");
        Notification oldest = new Notification();
        oldest.setRecipientID(12L);
        oldest.setMessage("Più vecchia");
        when(notificationRepository.findTop200ByRecipientIDOrderByCreatedAtDesc(12L))
            .thenReturn(List.of(newest, oldest));

        List<NotificationDTO> result = notificationQueryService.listForRecipient(12L);

        assertEquals("Più recente", result.get(0).getMessage());
        assertEquals("Più vecchia", result.get(1).getMessage());
        verify(notificationRepository).findTop200ByRecipientIDOrderByCreatedAtDesc(12L);
    }

    @Test
    void markAsRead_setsReadFlagAndSaves() {
        Notification notification = new Notification();
        notification.setNotificationID(5L);
        notification.setRecipientID(7L);
        notification.setRead(false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        notificationQueryService.markAsRead(5L, 7L);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadWrongRecipient() {
        Notification notification = new Notification();
        notification.setNotificationID(5L);
        notification.setRecipientID(99L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> notificationQueryService.markAsRead(5L, 7L));

        assertEquals(ResourceMessage.notificationNotFound(5L), exception.getMessage());
    }
}
