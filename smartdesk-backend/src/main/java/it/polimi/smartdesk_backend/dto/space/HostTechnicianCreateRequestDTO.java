package it.polimi.smartdesk_backend.dto.space;

import it.polimi.smartdesk_backend.util.validation.StrongPassword;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creazione tecnico da dashboard host (credenziali iniziali e specializzazione). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostTechnicianCreateRequestDTO {

    @NotBlank(message = "Il nome e cognome sono obbligatori.")
    @Size(max = 200, message = "Il nome non può superare i 200 caratteri.")
    private String name;

    @NotBlank(message = "L'email è obbligatoria.")
    @Email(message = "Inserisci un'email valida (es. nome@dominio.it).")
    @Size(max = 255, message = "L'email è troppo lunga.")
    private String email;

    @NotBlank(message = "La password è obbligatoria.")
    @Size(min = 8, max = 128, message = "La password deve avere tra 8 e 128 caratteri.")
    @StrongPassword
    private String password;

    @NotBlank(message = "La specializzazione è obbligatoria.")
    @Size(max = 500, message = "La specializzazione non può superare i 500 caratteri.")
    private String specialization;
}

