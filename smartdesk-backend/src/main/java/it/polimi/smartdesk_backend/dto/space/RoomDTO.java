package it.polimi.smartdesk_backend.dto.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Stanza all'interno di uno spazio: raggruppa postazioni con nome e codice breve. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {

    /** Identificativo interno stanza. */
    private Long roomID;

    /** Spazio di appartenenza. */
    private Long spaceID;

    @NotBlank
    @Size(max = 80)
    private String name;

    /** Codice stanza univoco nello spazio. */
    @NotBlank
    @Size(max = 10)
    private String code;
}

