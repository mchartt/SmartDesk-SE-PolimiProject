package it.polimi.smartdesk_backend.dto.auth;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Richiesta refresh sessione tramite refresh token opaco. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    /** UUID del refresh token ruotabile. */
    @NotNull(message = "Il refresh token è obbligatorio.")
    private UUID refreshToken;

}

