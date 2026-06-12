package it.polimi.smartdesk_backend.dto.space;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Spazio coworking assegnato a un tecnico (nome e codice ufficio per filtri UI). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianAssignedSpaceDTO {

    private Long spaceID;
    private String name;
    /** Codice ufficio opaco dello spazio. */
    private String officeCode;
}

