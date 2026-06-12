package it.polimi.smartdesk_backend.model.ticket;

import java.util.Set;

/** Stati ticket: OPEN → IN_PROGRESS → VERIFYING → RESOLVED. */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    VERIFYING,
    RESOLVED,
    CLOSED;

    /**
     * @return {@code true} se la transizione single-hop verso {@code next} è in {@link #allowedFrom(TicketStatus)}
     */
    public boolean canTransitionTo(TicketStatus next) {
        return allowedFrom(this).contains(next);
    }

    /** Transizioni ammesse in un solo hop dallo stato corrente. */
    public static Set<TicketStatus> allowedFrom(TicketStatus current) {
        return switch (current) {
            case OPEN -> Set.of(IN_PROGRESS);
            case IN_PROGRESS -> Set.of(VERIFYING, OPEN);
            case VERIFYING -> Set.of(RESOLVED, IN_PROGRESS, OPEN, CLOSED);
            case RESOLVED, CLOSED -> Set.of();
        };
    }

    /**
     * @param value nome enum case-insensitive; null/blank → OPEN
     * @throws IllegalArgumentException valore sconosciuto
     */
    public static TicketStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        for (TicketStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Stato ticket non supportato: " + value);
    }
}

