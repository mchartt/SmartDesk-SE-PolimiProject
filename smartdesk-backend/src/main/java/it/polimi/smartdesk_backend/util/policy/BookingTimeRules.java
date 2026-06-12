package it.polimi.smartdesk_backend.util.policy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

/** Politiche temporali condivise tra creazione e spostamento prenotazioni: finestra dei 7 giorni e margine sul primo slot. */
@Component
public class BookingTimeRules {

    /** Giorni prenotabili da oggi (incluso) fino a oggi + questo offset (incluso). Allineato a {@link it.polimi.smartdesk_backend.util.message.BookingMessage#BOOKING_DAY_WINDOW}. */
    public static final int MAX_BOOKING_DAYS_AHEAD = 7;

    /**
     * @return {@code true} se {@code bookingDay} è tra {@code today} e oggi + {@link #MAX_BOOKING_DAYS_AHEAD}
     */
    public boolean isBookingDayAllowed(LocalDate bookingDay, LocalDate today) {
        LocalDate lastBookableDay = today.plusDays(MAX_BOOKING_DAYS_AHEAD);
        return !bookingDay.isBefore(today) && !bookingDay.isAfter(lastBookableDay);
    }

    /** {@code true} se mancano almeno 30 minuti all'inizio slot ({@code slotStart} non precede {@code now + 30min}). Non sostituisce il controllo "nel futuro": un {@code slotStart} nel passato ritorna {@code false}. */
    public boolean firstBookingSlotStillOpen(LocalDateTime slotStart, LocalDateTime now) {
        return !slotStart.isBefore(now.plusMinutes(30));
    }
}
