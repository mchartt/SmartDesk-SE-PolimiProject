package it.polimi.smartdesk_backend.model.ticket;

/** Priorità ticket per UI/ordinamento; default MEDIUM se assente in input. */
public enum TicketSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** Parse case-insensitive; null/blank → MEDIUM (default difensivo). */
    public static TicketSeverity fromValue(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        for (TicketSeverity severity : values()) {
            if (severity.name().equalsIgnoreCase(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Gravità ticket non supportata: " + value);
    }
}

