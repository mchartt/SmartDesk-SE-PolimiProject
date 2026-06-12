package it.polimi.smartdesk_backend.dto.auth;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/** Risposta login/refresh: coppia token e scadenza access; {@code expiresIn} è un {@link java.time.Instant} (nome storico mantenuto per compatibilità client). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    /** Schema token (tipicamente {@code Bearer}). */
    private String tokenType = "Bearer";
    /** Istante di scadenza access token (o riferimento TTL lato client). */
    private Instant expiresIn;
    /** Identificativo utente. */
    private Long userID;
    /** Ruolo principale serializzato. */
    private String role;

}

