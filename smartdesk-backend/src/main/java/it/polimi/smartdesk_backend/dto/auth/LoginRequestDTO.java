package it.polimi.smartdesk_backend.dto.auth;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenziali per login (email + password). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @Email(message = "L'email non è valida.")
    @NotBlank(message = "L'email è obbligatoria.")
    private String email;

    @NotBlank(message = "La password è obbligatoria.")
    private String password;

}

