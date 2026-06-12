package it.polimi.smartdesk_backend.service.notification;
import it.polimi.smartdesk_backend.mapper.NotificationMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.dto.notification.NotificationUpdatedEventDTO;
import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationDomainServicesTest {

  // --- NotificationStreamHub ---

    private NotificationStreamHub notificationStreamHub;

    @BeforeEach
    void initNotificationTests() {
        notificationStreamHub = new NotificationStreamHub(new ObjectMapper());
        broadcaster = new NotificationRealtimeBroadcaster(notificationRepository, notificationStreamHub);
    }

    @Test
    void shouldConnectAndPublishUnreadCount() {
        SseEmitter emitter = notificationStreamHub.connect(4L, 2L);

        notificationStreamHub.publishUnreadCount(4L, 5L);

        assertNotNull(emitter);
    }

    @Test
    void shouldPublishNotificationCreatedToConnectedUser() {
        notificationStreamHub.connect(4L, 0L);

        notificationStreamHub.publishNotificationCreated(4L, NotificationMapper.fromNotification(sampleNotification()));

        notificationStreamHub.publishNotificationUpdated(4L,
                new NotificationUpdatedEventDTO(9L, true));
        notificationStreamHub.publishAllMarkedRead(4L);
    }

    @Test
    void shouldIgnorePublishWhenNoSubscribers() {
        notificationStreamHub.publishUnreadCount(99L, 1L);
    }

  // --- NotificationRealtimeBroadcaster ---

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationRealtimeBroadcaster broadcaster;

    @Test
    void shouldPublishCreatedNotificationAndUnreadCount() {
        Notification notification = sampleNotification();
        when(notificationRepository.countByRecipientIDAndReadFalse(4L)).thenReturn(3L);

        broadcaster.publishCreated(notification);

        verify(notificationRepository).countByRecipientIDAndReadFalse(4L);
    }

    @Test
    void shouldSkipPublishWhenRecipientMissing() {
        broadcaster.publishCreated(null);
        broadcaster.publishMarkedRead(null);
        broadcaster.publishForRecipient(null);
        verify(notificationRepository, never()).countByRecipientIDAndReadFalse(any());
    }

    @Test
    void shouldPublishMarkedReadAndAllRead() {
        Notification notification = sampleNotification();
        when(notificationRepository.countByRecipientIDAndReadFalse(4L)).thenReturn(1L);

        broadcaster.publishMarkedRead(notification);
        broadcaster.publishAllMarkedRead(4L);

        verify(notificationRepository, atLeastOnce())
                .countByRecipientIDAndReadFalse(eq(4L));
    }

    private static Notification sampleNotification() {
        Notification n = new Notification();
        n.setNotificationID(9L);
        n.setRecipientID(4L);
        n.setMessage("Test");
        n.setKind("TEST");
        n.setRead(false);
        return n;
    }
}
