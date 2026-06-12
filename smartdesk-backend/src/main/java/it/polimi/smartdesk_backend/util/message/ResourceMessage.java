package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Template centralizzati per “risorsa non trovata” con placeholder sull'id. I factory {@code *NotFound(long)} evitano di ripetere {@link String#formatted} nei service. */
@RequiredArgsConstructor
public enum ResourceMessage {
    DESK_NOT_FOUND("Postazione non trovata (id=%d)."),
    SPACE_NOT_FOUND("Spazio non trovato (id=%d)."),
    BOOKING_NOT_FOUND("Prenotazione non trovata (id=%d)."),
    USER_NOT_FOUND("Utente non trovato (id=%d)."),
    HOST_NOT_FOUND("Host non trovato (id=%d)."),
    ROOM_NOT_FOUND("Stanza non trovata (id=%d)."),
    TECHNICIAN_NOT_FOUND("Tecnico non trovato (id=%d)."),
    TICKET_NOT_FOUND("Ticket non trovato (id=%d)."),
    AMENITY_PRESET_NOT_FOUND("Preset servizi non trovato (id=%d)."),
    CLOSURE_NOT_FOUND("Chiusura non trovata (id=%d)."),
    NOTIFICATION_NOT_FOUND("Notifica non trovata (id=%d)."),
    REVIEW_NOT_FOUND("Recensione non trovata (id=%d).");

    private final String template;

    /** Sostituisce il placeholder {@code %d} del template con l'id richiesto. */
    public String format(long id) {
        return template.formatted(id);
    }

    public static String deskNotFound(long deskId) {
        return DESK_NOT_FOUND.format(deskId);
    }

    public static String deskNotFound(Long deskId) {
        return deskNotFound(idOrZero(deskId));
    }

    public static String spaceNotFound(long spaceId) {
        return SPACE_NOT_FOUND.format(spaceId);
    }

    public static String spaceNotFound(Long spaceId) {
        return spaceNotFound(idOrZero(spaceId));
    }

    public static String bookingNotFound(long bookingId) {
        return BOOKING_NOT_FOUND.format(bookingId);
    }

    public static String bookingNotFound(Long bookingId) {
        return bookingNotFound(idOrZero(bookingId));
    }

    public static String userNotFound(long userId) {
        return USER_NOT_FOUND.format(userId);
    }

    public static String userNotFound(Long userId) {
        return userNotFound(idOrZero(userId));
    }

    public static String hostNotFound(long hostId) {
        return HOST_NOT_FOUND.format(hostId);
    }

    public static String hostNotFound(Long hostId) {
        return hostNotFound(idOrZero(hostId));
    }

    public static String roomNotFound(long roomId) {
        return ROOM_NOT_FOUND.format(roomId);
    }

    public static String roomNotFound(Long roomId) {
        return roomNotFound(idOrZero(roomId));
    }

    public static String technicianNotFound(long technicianId) {
        return TECHNICIAN_NOT_FOUND.format(technicianId);
    }

    public static String technicianNotFound(Long technicianId) {
        return technicianNotFound(idOrZero(technicianId));
    }

    /** Tecnico assente nello spazio indicato. */
    public static String technicianNotFoundInSpace(long technicianId, long spaceId) {
        return "Tecnico non trovato (id=" + technicianId + ") nello spazio " + spaceId + ".";
    }

    public static String ticketNotFound(long ticketId) {
        return TICKET_NOT_FOUND.format(ticketId);
    }

    public static String ticketNotFound(Long ticketId) {
        return ticketNotFound(idOrZero(ticketId));
    }

    public static String amenityPresetNotFound(long presetId) {
        return AMENITY_PRESET_NOT_FOUND.format(presetId);
    }

    public static String closureNotFound(long closureId) {
        return CLOSURE_NOT_FOUND.format(closureId);
    }

    public static String notificationNotFound(long notificationId) {
        return NOTIFICATION_NOT_FOUND.format(notificationId);
    }

    public static String reviewNotFound(long reviewId) {
        return REVIEW_NOT_FOUND.format(reviewId);
    }

    public static String reviewNotFound(Long reviewId) {
        return reviewNotFound(idOrZero(reviewId));
    }

    private static long idOrZero(Long id) {
        return id == null ? 0L : id;
    }
}
