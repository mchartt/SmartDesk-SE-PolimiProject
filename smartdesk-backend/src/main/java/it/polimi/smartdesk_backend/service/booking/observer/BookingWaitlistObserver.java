package it.polimi.smartdesk_backend.service.booking.observer;

import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.service.booking.BookingWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Handler {@link BookingReleasedEvent}: notifica waitlist solo dopo commit della transazione che libera lo slot. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWaitlistObserver {
    private final BookingWaitlistService bookingWaitlistService;

    /** Fallimento post-commit → {@link RuntimeException}: lo slot è già libero ma la waitlist non è stata elaborata. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(BookingReleasedEvent event) {
        try {
            bookingWaitlistService.handleReleasedSlot(event.deskId(), event.day(), event.startTime(), event.endTime());
        } catch (Exception ex) {
            // BUG FIX: Poiché siamo in AFTER_COMMIT, la transazione originale è già stata committata con successo.
            // Rilanciare un'eccezione qui causerebbe un errore 500 al client per un'operazione (cancellazione) che è andata a buon fine.
            // Logghiamo l'errore come critico per intervento manuale/audit, ma non interrompiamo il flusso della risposta.
            log.error("FALLIMENTO POST-COMMIT: Impossibile processare waitlist per desk {} il {}. Gli utenti in attesa non saranno notificati automaticamente!", 
                    event.deskId(), event.day(), ex);
        }
    }
}

