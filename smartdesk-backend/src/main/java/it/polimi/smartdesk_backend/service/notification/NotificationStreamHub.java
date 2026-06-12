package it.polimi.smartdesk_backend.service.notification;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.notification.UnreadCountEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Connessioni SSE attive per utente: push conteggio unread e singole notifiche. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStreamHub {

    static final String EVENT_UNREAD_COUNT = "unread-count";
    static final String EVENT_NOTIFICATION_CREATED = "notification-created";
    static final String EVENT_NOTIFICATION_UPDATED = "notification-updated";
    static final String EVENT_ALL_READ = "notifications-all-read";

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /** Apre uno stream per {@code userId} e invia subito il conteggio corrente. */
    public SseEmitter connect(Long userId, long initialUnreadCount) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        subscribers.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        sendEvent(emitter, EVENT_UNREAD_COUNT, new UnreadCountEventDTO(initialUnreadCount));
        return emitter;
    }

    /** Notifica tutti i tab/browser connessi dello stesso utente. */
    public void publishUnreadCount(Long userId, long unreadCount) {
        publishEvent(userId, EVENT_UNREAD_COUNT, new UnreadCountEventDTO(unreadCount));
    }

    /** Invia una nuova notifica appena persistita a tutti i client connessi dell'utente. */
    public void publishNotificationCreated(Long userId, Object payload) {
        publishEvent(userId, EVENT_NOTIFICATION_CREATED, payload);
    }

    /** Aggiorna in push una notifica già esistente (es. dopo mark-as-read singolo). */
    public void publishNotificationUpdated(Long userId, Object payload) {
        publishEvent(userId, EVENT_NOTIFICATION_UPDATED, payload);
    }

    /** Evento bulk: tutte le notifiche sono state segnate come lette. */
    public void publishAllMarkedRead(Long userId) {
        publishEvent(userId, EVENT_ALL_READ, Map.of());
    }

    private void publishEvent(Long userId, String eventName, Object payload) {
        List<SseEmitter> emitters = subscribers.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, eventName, payload);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(serialize(payload)));
        } catch (IOException | IllegalStateException ex) {
            log.debug("SSE send failed, dropping emitter", ex);
            emitter.complete();
        }
    }

    private String serialize(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.computeIfPresent(userId, (key, list) -> list.isEmpty() ? null : list);
        }
    }
}
