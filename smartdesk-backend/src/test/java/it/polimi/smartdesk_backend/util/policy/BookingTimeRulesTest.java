package it.polimi.smartdesk_backend.util.policy;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica vincoli temporali prenotazione in {@link it.polimi.smartdesk_backend.util.regole.BookingTimeRules}. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class BookingTimeRulesTest {

    @InjectMocks
    private BookingTimeRules bookingTimeRules;

    @Test
    void firstSlotOpenUntilFirstHalfHourIsOver() {
        LocalDateTime slotStart = LocalDateTime.of(2026, 5, 20, 9, 0);

        assertFalse(bookingTimeRules.firstBookingSlotStillOpen(slotStart, slotStart.minusMinutes(29)));
        assertTrue(bookingTimeRules.firstBookingSlotStillOpen(slotStart, slotStart.minusMinutes(30)));
        assertFalse(bookingTimeRules.firstBookingSlotStillOpen(slotStart, slotStart.minusMinutes(1)));
        assertFalse(bookingTimeRules.firstBookingSlotStillOpen(slotStart, slotStart.plusMinutes(5)));
    }

    @Test
    void bookingWindowSevenDaysFromToday() {
        LocalDate today = LocalDate.of(2026, 5, 19);

        assertTrue(bookingTimeRules.isBookingDayAllowed(today, today));
        assertTrue(bookingTimeRules.isBookingDayAllowed(today.plusDays(7), today));
        assertFalse(bookingTimeRules.isBookingDayAllowed(today.minusDays(1), today));
        assertFalse(bookingTimeRules.isBookingDayAllowed(today.plusDays(8), today));
    }
}
