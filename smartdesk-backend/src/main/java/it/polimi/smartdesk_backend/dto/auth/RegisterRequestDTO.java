package it.polimi.smartdesk_backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import it.polimi.smartdesk_backend.util.validation.StrongPassword;

/** Payload registrazione worker: validazione formato su nome, cognome ed email. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "Il nome è obbligatorio.")
    @Size(min = 1, max = 80, message = "Il nome deve avere tra 1 e 80 caratteri.")
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N}\\s'.-]{0,79}$", message = "Il nome contiene caratteri non ammessi.")
    private String name;

    @NotBlank(message = "Il cognome è obbligatorio.")
    @Size(min = 1, max = 80, message = "Il cognome deve avere tra 1 e 80 caratteri.")
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N}\\s'.-]{0,79}$", message = "Il cognome contiene caratteri non ammessi.")
    private String surname;

    @Email(message = "L'email non è valida.")
    @NotBlank(message = "L'email è obbligatoria.")
    @Size(max = 255, message = "L'email è troppo lunga.")
    private String email;

    @NotBlank(message = "La password è obbligatoria.")
    @Size(min = 8, max = 128, message = "La password deve avere tra 8 e 128 caratteri.")
    @StrongPassword
    private String password;

    /** Ruolo richiesto in fase di registrazione (es. {@code WORKER}). */
    @NotBlank(message = "Il ruolo è obbligatorio.")
    private String role;

    private String company;

    /** Bio worker; alias JSON accettato: {@code description}. */
    @JsonAlias("description")
    private String bio;

}

