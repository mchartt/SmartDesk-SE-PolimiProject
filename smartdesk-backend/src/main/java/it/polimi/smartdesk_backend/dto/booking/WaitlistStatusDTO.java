package it.polimi.smartdesk_backend.dto.booking;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/** Stato iscrizione waitlist worker per una postazione e giorno. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistStatusDTO {

    /** Postazione monitorata. */
    private Long deskID;
    private LocalDate date;
    /** Se il worker è in lista d'attesa. */
    private boolean subscribed;
    /** Se è già stata inviata una notifica di disponibilità. */
    private boolean notified;

}

