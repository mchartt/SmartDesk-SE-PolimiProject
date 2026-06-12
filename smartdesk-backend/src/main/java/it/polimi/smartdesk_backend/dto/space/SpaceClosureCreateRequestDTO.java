package it.polimi.smartdesk_backend.dto.space;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Richiesta host: uno o più giorni chiusi con motivazione comune. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceClosureCreateRequestDTO {

    @NotEmpty(message = "Indica almeno un giorno di chiusura.")
    private List<LocalDate> dates = new ArrayList<>();

    @NotBlank(message = "Il motivo della chiusura è obbligatorio.")
    @Size(max = 2000, message = "Il motivo non può superare i 2000 caratteri.")
    private String reason;
}

