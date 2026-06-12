package it.polimi.smartdesk_backend.event;

import java.time.LocalDate;

import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Evento per avvisare il worker quando l'host chiude una sede e le prenotazioni vengono annullate. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BookingCancelledByHostNoticeEvent {
    Long workerId;
    String bookingRef;
    String spaceName;
    LocalDate bookedDay;
    String reason;
}
