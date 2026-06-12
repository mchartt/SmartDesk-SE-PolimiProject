package it.polimi.smartdesk_backend.service.notification;

import java.time.LocalDate;

/** SPI per notifiche in-app su eventi di dominio (booking, waitlist, ticket, moderazione). */
public interface NotificationService {

    /** Conferma al worker che la waitlist ha registrato la richiesta (testo generico lato implementazione). */
    void notifyWaitlistSubscription(Long workerId, Long deskId, LocalDate day);

    /** Notifica disponibilità di uno slot prenotabile, tipicamente dopo cancellazione o fine manutenzione. */
    void notifyDeskAvailability(Long workerId, Long deskId, LocalDate day);

    /** Notifica cancellazione prenotazione da sys admin; {@code bookingRef} è codice pubblico o fallback leggibile. */
    void notifyBookingCancelledByAdmin(Long workerId, String bookingRef);

    /** Chiusura straordinaria sede: include nome spazio, giorno e motivo così il worker capisce perché il booking è saltato. */
    void notifyBookingCancelledByHost(
            Long workerId, String bookingRef, String spaceName, LocalDate bookedDay, String reason);

    /** Notifica esito revisione admin su uno spazio. */
    void notifySpaceDecision(Long hostId, String spaceName, String decision);

    /** Avvisa il worker che una prenotazione futura tocca una postazione in manutenzione. */
    void notifyDeskMaintenanceWarning(Long workerId, String bookingRef, String deskLabel, LocalDate bookedDay);

    /** Avvisa il worker che la postazione della prenotazione è stata ripristinata. */
    void notifyDeskMaintenanceRestored(Long workerId, String bookingRef, String deskLabel, LocalDate bookedDay);

    /** Notifica al worker che un tecnico ha preso in carico il ticket. */
    void notifyTicketAssigned(Long workerId, String ticketCode);

    /** L'host ha assegnato al tecnico una segnalazione da gestire. */
    void notifyTicketAssignedToTechnician(Long technicianId, String ticketCode);

    /** Ticket chiuso con esito positivo (o comunque risolto lato processo). */
    void notifyTicketResolved(Long workerId, String ticketCode);

    /** Ticket in verifica dall'host. */
    void notifyTicketVerifying(Long workerId, String ticketCode);

    /** Ticket tornato in lavorazione (respinto da host o riassegnato). */
    void notifyTicketInProgress(Long workerId, String ticketCode);

    /** Nuovo messaggio in chat ticket per il worker: titolo con mittente e ticket (titolo troncato se lungo). */
    void notifyTicketNoteUpdated(
            Long recipientId, Long authorUserId, String ticketTitle, String ticketCode);

    /** Segnalazione in VERIFYING: il tecnico ha completato la riparazione, l'host deve approvare. */
    void notifyHostTicketNeedsApproval(Long hostId, String ticketCode);

    /** Notifica generica con messaggio arbitrario senza {@code kind} strutturato. */
    void sendUserNotification(Long recipientId, String message);

    /** Evento ricco verso host/admin: {@code kind} stabile per filtri UI (es. {@code HOST_TICKET_OPENED}), più metadati attore. {@code actorRating} opzionale (es. rating worker quando ha senso nel contesto). */
    void sendWorkerActivityNotification(
            Long recipientId,
            String message,
            String kind,
            String actorName,
            String actorSurname,
            String actorEmail,
            Integer actorRating);
}

