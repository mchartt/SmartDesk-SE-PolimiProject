package it.polimi.smartdesk_backend.dto.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Dati anagrafici modificabili dal profilo utente (nome, cognome, email con validazione Jakarta). */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class UpdateProfileRequestBody {
    @NotBlank(message = "Il nome è obbligatorio.")
    @Size(max = 80, message = "Il nome non può superare gli 80 caratteri.")
    String name;
    @NotBlank(message = "Il cognome è obbligatorio.")
    @Size(max = 80, message = "Il cognome non può superare gli 80 caratteri.")
    String surname;
    @NotBlank(message = "L'email è obbligatoria.")
    @Email(message = "L'email non è valida.")
    String email;
}

