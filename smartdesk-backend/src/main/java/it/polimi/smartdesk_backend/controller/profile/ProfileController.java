package it.polimi.smartdesk_backend.controller.profile;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.dto.common.ChangePasswordRequest;
import it.polimi.smartdesk_backend.dto.common.UpdateProfileRequestBody;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.notification.NotificationQueryService;
import it.polimi.smartdesk_backend.service.notification.NotificationStreamHub;
import it.polimi.smartdesk_backend.service.profile.ProfileService;
import it.polimi.smartdesk_backend.util.support.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Profilo utente autenticato, inbox notifiche, cambio password e cancellazione account. */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profilo", description = "Profilo utente corrente, notifiche, password e cancellazione account.")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AccessControlService accessControlService;
    private final NotificationQueryService notificationQueryService;
    private final NotificationStreamHub notificationStreamHub;

    /** Restituisce il riepilogo del profilo dell'utente autenticato. */
    @GetMapping
    @Operation(summary = "Legge il profilo utente corrente")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(profileService.getProfile(requester.getUserId()));
    }

    /** Aggiorna i dati anagrafici dell'utente autenticato (nome, cognome o email). */
    @PutMapping
    @Operation(summary = "Aggiorna profilo (nome, cognome, email)")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequestBody request) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(profileService.updateProfile(requester.getUserId(), request.name(), request.surname(),
                request.email()));
    }

    /** Elenca le notifiche ricevute dall'utente autenticato (prenotazioni confermate, feedback, ecc.). */
    @GetMapping("/notifications")
    @Operation(summary = "Elenca le notifiche dell'utente corrente")
    public ResponseEntity<List<NotificationDTO>> listNotifications(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(notificationQueryService.listForRecipient(requester.getUserId()));
    }

    /** Contrassegna una singola notifica dell'utente autenticato come letta. */
    @PatchMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Segna una notifica come letta")
    public ResponseEntity<Void> markNotificationRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        notificationQueryService.markAsRead(notificationId, requester.getUserId());
        return ResponseEntity.noContent().build();
    }

    /** Contrassegna tutte le notifiche dell'utente autenticato come lette. */
    @PatchMapping("/notifications/read-all")
    @Operation(summary = "Segna tutte le notifiche come lette")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        notificationQueryService.markAllAsRead(requester.getUserId());
        return ResponseEntity.noContent().build();
    }

    /** Elimina dallo storico tutte le notifiche già lette dell'utente autenticato. */
    @DeleteMapping("/notifications/history")
    @Operation(summary = "Elimina lo storico notifiche lette")
    public ResponseEntity<Void> clearReadHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        notificationQueryService.clearReadHistory(requester.getUserId());
        return ResponseEntity.noContent().build();
    }

    /** Restituisce il conteggio delle notifiche non lette dell'utente autenticato. */
    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Conteggio notifiche non lette")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(notificationQueryService.unreadCount(requester.getUserId()));
    }

    /** Stream SSE: il server invia eventi {@code unread-count} quando cambia il badge. Richiede header {@code Authorization: Bearer …} (come le altre API). */
    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE del conteggio non lette in tempo reale")
    public SseEmitter notificationStream(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        long count = notificationQueryService.unreadCount(requester.getUserId());
        return notificationStreamHub.connect(requester.getUserId(), count);
    }

    /** Aggiorna la password dell'utente autenticato: richiede password corrente e nuova. Non consente il reset senza sessione attiva. */
    @PutMapping("/password")
    @Operation(summary = "Cambia la password dell'utente corrente")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        var requester = accessControlService.assertAuthenticated(principal);
        profileService.changePassword(requester.getUserId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /** Elimina definitivamente l'account dell'utente autenticato. */
    @DeleteMapping
    @Operation(summary = "Elimina definitivamente l'account utente corrente")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest) {
        var requester = accessControlService.assertAuthenticated(principal);
        profileService.deleteAccount(requester.getUserId(), RequestUtils.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }
}


