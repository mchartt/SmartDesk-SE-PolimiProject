package it.polimi.smartdesk_backend.dto.space;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Chiusura temporanea ufficio (un giorno di calendario). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceClosureDTO {

    private Long id;
    private Long spaceID;
    private LocalDate closedDate;
    private String reason;
}

