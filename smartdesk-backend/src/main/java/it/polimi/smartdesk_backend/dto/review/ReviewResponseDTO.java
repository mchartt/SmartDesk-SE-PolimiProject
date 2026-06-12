package it.polimi.smartdesk_backend.dto.review;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/** Recensione serializzata per le API. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {

    private Long reviewID;
    private Long workerID;
    private Long hostID;
    private Long spaceID;
    private Long bookingID;
    private String spaceOfficeCode;
    private String spaceName;
    private String city;
    private String workerGivenName;
    private String workerFamilyName;
    private String workerEmail;
    private int rating;
    private String comment;
    private LocalDate createdAt;
    private boolean seenByHost;


}

