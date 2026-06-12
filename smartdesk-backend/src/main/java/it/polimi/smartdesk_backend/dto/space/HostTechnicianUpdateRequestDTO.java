package it.polimi.smartdesk_backend.dto.space;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Aggiornamento profilo tecnico da host (controllo versione ottimistica). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostTechnicianUpdateRequestDTO {

    @NotBlank(message = "Il nome è obbligatorio.")
    private String name;

    @Email(message = "L'email non è valida.")
    @NotBlank(message = "L'email è obbligatoria.")
    private String email;

    private String specialization;

    /** Nuova password; vuoto mantiene l'hash esistente. */
    private String password;

    /** Versione riga utente per aggiornamento ottimistico (deve coincidere con quanto letto dal client). */
    @NotNull(message = "profileVersion è obbligatorio.")
    private Long profileVersion;
}

