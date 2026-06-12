package it.polimi.smartdesk_backend.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Evento emesso quando uno slot prenotabile si libera (cancellazione o riprogrammazione); consumato dalla waitlist post-commit. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BookingReleasedEvent {
    Long deskId;
    LocalDate day;
    LocalDateTime startTime;
    LocalDateTime endTime;
}
