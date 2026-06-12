package it.polimi.smartdesk_backend.dto.common;

import jakarta.validation.constraints.NotBlank;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Corpo per il cambio password: verifica quella attuale e applica la nuova secondo regole admin. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ChangePasswordRequest {
    @NotBlank(message = "La password attuale è obbligatoria.")
    String currentPassword;
    @NotBlank(message = "La nuova password è obbligatoria.")
    String newPassword;
}

