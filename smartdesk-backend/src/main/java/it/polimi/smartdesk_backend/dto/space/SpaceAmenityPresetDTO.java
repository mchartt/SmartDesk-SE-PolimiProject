package it.polimi.smartdesk_backend.dto.space;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Preset di dotazioni riutilizzabile per creare postazioni omogenee nello stesso spazio. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceAmenityPresetDTO {

    /** Identificativo preset. */
    private Long presetID;

    /** Spazio di appartenenza. */
    private Long spaceID;

    @NotBlank
    @Size(max = 48)
    private String label;

    /** Suggerimento breve per l'host (testo opzionale). */
    @Size(max = 120)
    private String hint;

    /** Lista codici amenity applicabili al preset (non vuota). */
    @NotEmpty
    private List<@NotBlank @Size(max = 12) String> amenities;
}

