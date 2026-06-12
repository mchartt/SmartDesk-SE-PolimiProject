package it.polimi.smartdesk_backend.model.space;

/** Stato operativo persistito su {@link Desk}. BOOKED legacy non usato: occupazione solo su {@code booking}. */
public enum DeskStateCode {
    AVAILABLE,
    RESERVED,
    MAINTENANCE,
    PENDING_INSPECTION,
    DECOMMISSIONED
}

