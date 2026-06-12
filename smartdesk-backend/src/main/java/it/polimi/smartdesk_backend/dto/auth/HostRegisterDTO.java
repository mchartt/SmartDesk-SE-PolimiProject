package it.polimi.smartdesk_backend.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import it.polimi.smartdesk_backend.util.validation.StrongPassword;

/** Payload registrazione host: anagrafica, dati fiscali e descrizione attività. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostRegisterDTO {

    @NotBlank(message = "Il nome è obbligatorio.")
    @Size(min = 1, max = 80)
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N}\\s'.-]{0,79}$", message = "Il nome contiene caratteri non validi.")
    private String name;

    @NotBlank(message = "Il cognome è obbligatorio.")
    @Size(min = 1, max = 80)
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N}\\s'.-]{0,79}$", message = "Il cognome contiene caratteri non validi.")
    private String surname;

    @Email(message = "L'email non è valida.")
    @NotBlank(message = "L'email è obbligatoria.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "La password è obbligatoria.")
    @Size(min = 8, max = 128, message = "La password deve avere tra 8 e 128 caratteri.")
    @StrongPassword
    private String password;

    @NotBlank(message = "Il nome della struttura è obbligatorio.")
    private String nameStructure;

    @NotBlank(message = "La partita IVA è obbligatoria.")
    private String vatNumber;

    @NotBlank(message = "La descrizione è obbligatoria.")
    @Size(min = 50, max = 2000, message = "La descrizione deve avere tra 50 e 2000 caratteri.")
    private String description;

}

