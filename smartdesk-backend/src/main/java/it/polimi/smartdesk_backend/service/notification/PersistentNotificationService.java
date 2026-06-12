package it.polimi.smartdesk_backend.service.notification;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import it.polimi.smartdesk_backend.model.notification.Notification;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.notification.NotificationRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.model.space.Desk;

/** Implementazione {@link NotificationService} che persiste ogni avviso in tabella {@code notification}. I testi e i {@code kind} sono fissati per tipo evento (waitlist, cancellazione host, ticket, ecc.). */
@Service
@Primary
@RequiredArgsConstructor
public class PersistentNotificationService implements NotificationService {

    private static final int TICKET_TITLE_NOTIFICATION_MAX = 48;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DeskRepository deskRepository;
    private final NotificationRealtimeBroadcaster realtimeBroadcaster;

    /** Conferma iscrizione waitlist. */
    @Override
    public void notifyWaitlistSubscription(Long workerId, Long deskId, LocalDate day) {
        String label = resolveDeskLabel(deskId);
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("WAITLIST_SUBSCRIBED");
        notification.setMessage(
                "Iscrizione alla lista d'attesa confermata per la postazione " + label + " nel giorno " + day + ".");
        persist(notification);
    }

    /** Avviso slot libero dopo cancellazione o sblocco compatibile con la waitlist. */
    @Override
    public void notifyDeskAvailability(Long workerId, Long deskId, LocalDate day) {
        String label = resolveDeskLabel(deskId);
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("DESK_AVAILABLE");
        notification.setMessage("La postazione " + label + " è di nuovo disponibile il " + day + ".");
        persist(notification);
    }

    /** Kind {@code BOOKING_CANCELLED}: annuncio admin; no-op se workerId nullo. */
    @Override
    public void notifySpaceDecision(Long hostId, String spaceName, String decision) {
        if (hostId == null) {
            return;
        }
        String space = spaceName == null || spaceName.isBlank() ? "Il tuo spazio" : spaceName.trim();
        String outcome = decision == null || decision.isBlank() ? "aggiornato" : decision.trim();
        Notification notification = new Notification();
        notification.setRecipientID(hostId);
        notification.setKind("SPACE_DECISION");
        notification.setMessage(space + ": stato aggiornato dall'amministratore (" + outcome + ").");
        persist(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyDeskMaintenanceWarning(Long workerId, String bookingRef, String deskLabel, LocalDate bookedDay) {
        if (workerId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("DESK_MAINTENANCE_WARNING");
        notification.setMessage("Attenzione, la postazione " + safe(deskLabel, "selezionata")
                + " è stata messa in manutenzione. La prenotazione " + bookingLabel(bookingRef)
                + " del " + dayLabel(bookedDay) + " non sarà garantita.");
        persist(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyDeskMaintenanceRestored(Long workerId, String bookingRef, String deskLabel, LocalDate bookedDay) {
        if (workerId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("DESK_MAINTENANCE_RESTORED");
        notification.setMessage("La postazione " + safe(deskLabel, "selezionata")
                + " è stata ripristinata. Puoi procedere senza problemi con la prenotazione "
                + bookingLabel(bookingRef) + " del " + dayLabel(bookedDay) + ".");
        persist(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyBookingCancelledByAdmin(Long workerId, String bookingRef) {
        if (workerId == null) {
            return;
        }
        String ref = bookingRef == null || bookingRef.isBlank() ? "—" : bookingRef.trim();
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("BOOKING_CANCELLED");
        notification.setMessage("La tua prenotazione #" + ref + " è stata annullata dall'amministratore.");
        persist(notification);
    }

    /** Tipo {@code BOOKING_CANCELLED_HOST_CLOSURE}: testo già composto in italiano con ripiego sui campi null. */
    @Override
    public void notifyBookingCancelledByHost(
            Long workerId, String bookingRef, String spaceName, LocalDate bookedDay, String reason) {
        if (workerId == null) {
            return;
        }
        String ref = bookingRef == null || bookingRef.isBlank() ? "—" : bookingRef.trim();
        String space = spaceName == null || spaceName.isBlank() ? "La sede" : spaceName.trim();
        String day = bookedDay == null ? "—" : bookedDay.toString();
        String why = reason == null || reason.isBlank() ? "—" : reason.trim();
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("BOOKING_CANCELLED_HOST_CLOSURE");
        notification.setMessage(
                space + " risulta chiusa il " + day + ". La prenotazione #" + ref + " è stata annullata. Motivo: " + why);
        persist(notification);
    }

    /** Notifica worker: un tecnico ha preso in carico la segnalazione. */
    @Override
    public void notifyTicketAssigned(Long workerId, String ticketCode) {
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setMessage("Il tuo ticket " + ticketCode + " è stato preso in carico da un tecnico.");
        persist(notification);
    }

    /** Notifica tecnico: l'host gli ha assegnato una segnalazione. */
    @Override
    public void notifyTicketAssignedToTechnician(Long technicianId, String ticketCode) {
        if (technicianId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(technicianId);
        notification.setKind("TICKET_ASSIGNED_TO_TECHNICIAN");
        notification.setMessage(
                "Ti è stata assegnata la segnalazione " + safe(ticketCode, "—") + " dall'host.");
        persist(notification);
    }

    /** Chiusura ticket lato worker: messaggio positivo breve. */
    @Override
    public void notifyTicketResolved(Long workerId, String ticketCode) {
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("TICKET_RESOLVED");
        notification.setMessage("La segnalazione " + safe(ticketCode, "—") + " è stata risolta e approvata.");
        persist(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTicketVerifying(Long workerId, String ticketCode) {
        if (workerId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("TICKET_VERIFYING");
        notification.setMessage("Il tecnico ha completato la riparazione per " + safe(ticketCode, "—") + ". In attesa di verifica dall'host.");
        persist(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTicketInProgress(Long workerId, String ticketCode) {
        if (workerId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(workerId);
        notification.setKind("TICKET_IN_PROGRESS");
        notification.setMessage("La segnalazione " + safe(ticketCode, "—") + " è tornata in fase di lavorazione.");
        persist(notification);
    }

    /** Avvisa il worker di un nuovo messaggio da tecnico o host sulla chat del ticket. */
    @Override
    public void notifyTicketNoteUpdated(
            Long recipientId, Long authorUserId, String ticketTitle, String ticketCode) {
        if (recipientId == null) {
            return;
        }
        User author = authorUserId != null ? userRepository.findById(authorUserId).orElse(null) : null;
        String actorName = author != null && author.getName() != null ? author.getName().trim() : "";
        String actorSurname = author != null && author.getSurname() != null ? author.getSurname().trim() : "";
        String actorEmail = author != null && author.getEmail() != null ? author.getEmail().trim() : "";
        String actorFullName = formatPersonName(actorName, actorSurname);
        if (actorFullName.isEmpty()) {
            actorFullName = "—";
        }
        String ticketHeading = ticketTitleForNotification(ticketTitle, ticketCode);
        String message =
                "Nuovo messaggio da " + actorFullName + " su Ticket: " + ticketHeading;

        Notification notification = new Notification();
        notification.setRecipientID(recipientId);
        notification.setKind("TICKET_NOTE_UPDATED");
        notification.setMessage(message);
        notification.setActorName(blankToNull(actorName));
        notification.setActorSurname(blankToNull(actorSurname));
        notification.setActorEmail(blankToNull(actorEmail));
        persist(notification);
    }

    /** Host: riparazione completata, serve approvazione (stato VERIFYING). */
    @Override
    public void notifyHostTicketNeedsApproval(Long hostId, String ticketCode) {
        if (hostId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientID(hostId);
        notification.setKind("HOST_TICKET_VERIFYING");
        notification.setMessage(
                "La segnalazione " + safe(ticketCode, "—")
                        + " è stata risolta dal tecnico ed è in attesa della tua approvazione.");
        persist(notification);
    }

    /** Inbox generica: nessun {@code kind} impostato—solo testo libero. */
    @Override
    public void sendUserNotification(Long recipientId, String message) {
        Notification notification = new Notification();
        notification.setRecipientID(recipientId);
        notification.setMessage(message);
        persist(notification);
    }

    /** Notifica ricca per host/admin: popola anche metadati attore (nome, mail, rating) oltre al {@code kind} per filtri UI. */
    @Override
    public void sendWorkerActivityNotification(
            Long recipientId,
            String message,
            String kind,
            String actorName,
            String actorSurname,
            String actorEmail,
            Integer actorRating) {
        Notification notification = new Notification();
        notification.setRecipientID(recipientId);
        notification.setMessage(message);
        notification.setKind(kind);
        notification.setActorName(blankToNull(actorName));
        notification.setActorSurname(blankToNull(actorSurname));
        notification.setActorEmail(blankToNull(actorEmail));
        notification.setActorRating(actorRating);
        persist(notification);
    }

    private void persist(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        realtimeBroadcaster.publishCreated(saved);
    }

    /** Normalizza stringhe vuote a {@code null} per non sporcare JSON con whitespace. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Codice postazione (es. A1); se assente ripiego sull'id numerico. */
    private String resolveDeskLabel(Long deskId) {
        if (deskId == null) {
            return "—";
        }
        return deskRepository.findById(deskId)
                .map(Desk::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .orElse(String.valueOf(deskId));
    }

    private static String formatPersonName(String name, String surname) {
        String n = name == null ? "" : name.trim();
        String s = surname == null ? "" : surname.trim();
        if (n.isEmpty() && s.isEmpty()) {
            return "";
        }
        if (n.isEmpty()) {
            return s;
        }
        if (s.isEmpty()) {
            return n;
        }
        return n + " " + s;
    }

    private static String ticketTitleForNotification(String title, String ticketCode) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) {
            return safe(ticketCode, "—");
        }
        if (t.length() <= TICKET_TITLE_NOTIFICATION_MAX) {
            return t;
        }
        return t.substring(0, TICKET_TITLE_NOTIFICATION_MAX).trim() + "…";
    }

    private static String bookingLabel(String bookingRef) {
        return bookingRef == null || bookingRef.isBlank() ? "" : "#" + bookingRef.trim();
    }

    private static String dayLabel(LocalDate day) {
        return day == null ? "indicato" : day.toString();
    }
}

