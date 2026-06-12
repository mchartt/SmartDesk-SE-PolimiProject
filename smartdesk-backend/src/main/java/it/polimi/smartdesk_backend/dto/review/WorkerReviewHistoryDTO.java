package it.polimi.smartdesk_backend.dto.review;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Voce dello storico recensioni lato worker (metadati prenotazione, testi, reazioni). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerReviewHistoryDTO {

    private Long reviewID;
    private Long bookingID;
    /** Codice pubblico prenotazione (6 caratteri), se disponibile. */
    private String bookingCode;
    private Long workerID;
    private Long hostID;
    private Long spaceID;
    private String spaceName;
    private String city;
    /** Codice ufficio spazio persistito, se caricato. */
    private String spaceOfficeCode;
    private int rating;
    private String comment;
    private LocalDate createdAt;

}

