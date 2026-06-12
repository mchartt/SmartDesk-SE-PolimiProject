package it.polimi.smartdesk_backend.model.booking;

/** Stato prenotazione persistito; query di disponibilità escludono {@code CANCELLED}. */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

