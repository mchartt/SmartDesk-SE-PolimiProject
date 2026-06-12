package it.polimi.smartdesk_backend.service.booking.observer;

import it.polimi.smartdesk_backend.event.BookingCancelledByHostNoticeEvent;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Persiste notifica cancellazione host su {@link BookingCancelledByHostNoticeEvent} dopo commit. */
@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class BookingCancelledByHostNoticeObserver {

    private final NotificationService notificationService;

    /** Invia la notifica di cancellazione host dopo il commit della transazione che persiste la cancellazione. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(BookingCancelledByHostNoticeEvent event) {
        try {
            notificationService.notifyBookingCancelledByHost(
                    event.workerId(),
                    event.bookingRef(),
                    event.spaceName(),
                    event.bookedDay(),
                    event.reason());
        } catch (Exception ex) {
            log.error("FALLIMENTO POST-COMMIT: notifica cancellazione host non inviata per booking {}", 
                    event.bookingRef(), ex);
        }
    }
}

