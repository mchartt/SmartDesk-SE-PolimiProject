package it.polimi.smartdesk_backend.dto.space;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Dati anagrafici e orari di apertura inviati dall’host quando crea o modifica una sede. */
@Data
public class SpaceRequestDTO {

    @NotBlank(message = "Il nome è obbligatorio.")
    @Size(max = 50, message = "Il nome non può superare i 50 caratteri.")
    private String name;

    @NotBlank(message = "L'indirizzo è obbligatorio.")
    @Size(max = 100, message = "L'indirizzo non può superare i 100 caratteri.")
    private String address;

    @NotBlank(message = "La città è obbligatoria.")
    @Size(max = 50, message = "La città non può superare i 50 caratteri.")
    private String city;

    @Size(max = 500, message = "La descrizione non può superare i 500 caratteri.")
    private String description;

    private Map<String, OpeningHoursDayDTO> openingHours;
}

