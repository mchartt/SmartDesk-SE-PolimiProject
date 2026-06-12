package it.polimi.smartdesk_backend.service.notification;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;

/** Creazione messaggi in-app persistenti (es. disponibilità desk, promemoria). */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class PersistentNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeskRepository deskRepository;

    @Mock
    private NotificationRealtimeBroadcaster realtimeBroadcaster;

    @InjectMocks
    private PersistentNotificationService persistentNotificationService;

    @Test
    void persistWaitlistAndAvailabilityNotifications() {
        LocalDate day = LocalDate.of(2026, 4, 29);
        stubDeskCode(10L, "A1");

        persistentNotificationService.notifyWaitlistSubscription(4L, 10L, day);
        persistentNotificationService.notifyDeskAvailability(4L, 10L, day);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertEquals(
                "Iscrizione alla lista d'attesa confermata per la postazione A1 nel giorno 2026-04-29.",
                captor.getAllValues().get(0).getMessage());
        assertEquals(
                "La postazione A1 è di nuovo disponibile il 2026-04-29.",
                captor.getAllValues().get(1).getMessage());
    }

    private void stubDeskCode(long deskId, String code) {
        Desk desk = new Desk();
        desk.setDeskID(deskId);
        desk.setCode(code);
        when(deskRepository.findById(deskId)).thenReturn(Optional.of(desk));
    }

    @Test
    void notificationContainsTheRightRecipient() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        persistentNotificationService.notifyDeskAvailability(9L, 3L, day);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        assertEquals(9L, captor.getValue().getRecipientID());
    }

    @Test
    void bookingCancelledByAdminNotification() {
        persistentNotificationService.notifyBookingCancelledByAdmin(4L, "ABC123");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(4L, n.getRecipientID());
        assertEquals("BOOKING_CANCELLED", n.getKind());
        assertEquals("La tua prenotazione #ABC123 è stata annullata dall'amministratore.", n.getMessage());
    }

    @Test
    void bookingCancelledByHostClosureNotification() {
        persistentNotificationService.notifyBookingCancelledByHost(
                4L, "XY12AB", "Coworking Test", LocalDate.of(2026, 6, 10), "Manutenzione impianti");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(4L, n.getRecipientID());
        assertEquals("BOOKING_CANCELLED_HOST_CLOSURE", n.getKind());
        assertEquals(
                "Coworking Test risulta chiusa il 2026-06-10. La prenotazione #XY12AB è stata annullata. Motivo: Manutenzione impianti",
                n.getMessage());
    }

    @Test
    void workerActivityNotificationStoresActorMetadata() {
        persistentNotificationService.sendWorkerActivityNotification(
                8L,
                "Riepilogo sintetico",
                "HOST_REVIEW_LEFT",
                "Ada ",
                "Lovelace",
                " ada@example.com ",
                4);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(8L, n.getRecipientID());
        assertEquals("HOST_REVIEW_LEFT", n.getKind());
        assertEquals("Ada", n.getActorName());
        assertEquals("Lovelace", n.getActorSurname());
        assertEquals("ada@example.com", n.getActorEmail());
        assertEquals(4, n.getActorRating());
        assertEquals("Riepilogo sintetico", n.getMessage());
    }

    @Test
    void ticketAssignedToTechnicianNotification() {
        persistentNotificationService.notifyTicketAssignedToTechnician(3L, "T001");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(3L, n.getRecipientID());
        assertEquals("TICKET_ASSIGNED_TO_TECHNICIAN", n.getKind());
        assertEquals("Ti è stata assegnata la segnalazione T001 dall'host.", n.getMessage());
    }

    @Test
    void shouldNotifySpaceDecisionForHost() {
        persistentNotificationService.notifySpaceDecision(8L, "Milano Hub", "approvato");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(8L, n.getRecipientID());
        assertEquals("SPACE_DECISION", n.getKind());
        assertEquals("Milano Hub: stato aggiornato dall'amministratore (approvato).", n.getMessage());
    }

    @Test
    void shouldSkipSpaceDecisionWhenHostIdNull() {
        persistentNotificationService.notifySpaceDecision(null, "Hub", "ok");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldNotifyTicketResolvedForWorker() {
        persistentNotificationService.notifyTicketResolved(4L, "T9999");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("TICKET_RESOLVED", captor.getValue().getKind());
        assertEquals("La segnalazione T9999 è stata risolta e approvata.", captor.getValue().getMessage());
    }

    @Test
    void shouldNotifyHostTicketNeedsApproval() {
        persistentNotificationService.notifyHostTicketNeedsApproval(8L, "T1111");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("HOST_TICKET_VERIFYING", captor.getValue().getKind());
    }

    @Test
    void shouldNotifyTicketAssignedToWorker() {
        persistentNotificationService.notifyTicketAssigned(4L, "T002");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(4L, captor.getValue().getRecipientID());
        assertEquals("Il tuo ticket T002 è stato preso in carico da un tecnico.", captor.getValue().getMessage());
    }

    @Test
    void shouldNotifyTicketVerifyingForWorker() {
        persistentNotificationService.notifyTicketVerifying(4L, "T003");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("TICKET_VERIFYING", captor.getValue().getKind());
    }

    @Test
    void shouldNotifyDeskMaintenanceWarning() {
        persistentNotificationService.notifyDeskMaintenanceWarning(
                4L, "XY12", "B2", LocalDate.of(2026, 8, 1));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("DESK_MAINTENANCE_WARNING", captor.getValue().getKind());
    }

    @Test
    void shouldNotifyDeskMaintenanceRestored() {
        persistentNotificationService.notifyDeskMaintenanceRestored(
                4L, "XY12", "B2", LocalDate.of(2026, 8, 2));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("DESK_MAINTENANCE_RESTORED", captor.getValue().getKind());
    }

    @Test
    void shouldSendGenericUserNotification() {
        persistentNotificationService.sendUserNotification(8L, "Messaggio generico");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Messaggio generico", captor.getValue().getMessage());
    }

    @Test
    void ticketNoteUpdatedNotification() {
        Technician author = new Technician();
        author.setName("Mario");
        author.setSurname("Rossi");
        author.setEmail("mario@example.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(author));

        persistentNotificationService.notifyTicketNoteUpdated(8L, 3L, "Problemi alla presa.", "T001");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(8L, n.getRecipientID());
        assertEquals("TICKET_NOTE_UPDATED", n.getKind());
        assertEquals("Nuovo messaggio da Mario Rossi su Ticket: Problemi alla presa.", n.getMessage());
        assertEquals("Mario", n.getActorName());
        assertEquals("Rossi", n.getActorSurname());
        assertEquals("mario@example.com", n.getActorEmail());
    }
}
