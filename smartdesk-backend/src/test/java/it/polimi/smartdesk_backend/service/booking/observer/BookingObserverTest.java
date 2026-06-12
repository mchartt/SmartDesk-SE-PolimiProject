package it.polimi.smartdesk_backend.service.booking.observer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.event.BookingCancelledByHostNoticeEvent;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.service.booking.BookingWaitlistService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class BookingObserverTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingCancelledByHostNoticeObserver bookingCancelledByHostNoticeObserver;

    @Mock
    private BookingWaitlistService bookingWaitlistService;

    @InjectMocks
    private BookingWaitlistObserver bookingWaitlistObserver;

    @Test
    void shouldNotifyWorkerOnHostCancellationEvent() {
        var event = new BookingCancelledByHostNoticeEvent(
                4L, "REF1", "Hub Milano", LocalDate.of(2026, 6, 1), "Chiusura straordinaria");

        bookingCancelledByHostNoticeObserver.onEvent(event);

        verify(notificationService).notifyBookingCancelledByHost(
                4L, "REF1", "Hub Milano", LocalDate.of(2026, 6, 1), "Chiusura straordinaria");
    }

    @Test
    void shouldSwallowNotificationFailureAfterCommit() {
        var event = new BookingCancelledByHostNoticeEvent(4L, "REF2", "Hub", LocalDate.now(), "Reason");
        doThrow(new RuntimeException("broker down"))
                .when(notificationService)
                .notifyBookingCancelledByHost(
                        event.workerId(),
                        event.bookingRef(),
                        event.spaceName(),
                        event.bookedDay(),
                        event.reason());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> bookingCancelledByHostNoticeObserver.onEvent(event));
    }

    @Test
    void shouldHandleReleasedSlotOnBookingReleasedEvent() {
        LocalDate day = LocalDate.of(2026, 7, 1);
        LocalDateTime start = day.atTime(10, 0);
        LocalDateTime end = day.atTime(12, 0);
        var event = new BookingReleasedEvent(12L, day, start, end);

        bookingWaitlistObserver.onEvent(event);

        verify(bookingWaitlistService).handleReleasedSlot(12L, day, start, end);
    }
}
