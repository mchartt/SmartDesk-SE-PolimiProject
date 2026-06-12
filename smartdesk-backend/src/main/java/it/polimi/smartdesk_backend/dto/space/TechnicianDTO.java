package it.polimi.smartdesk_backend.dto.space;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Tecnico assegnato all'host: identità, versione riga e spazi collegati. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDTO {

    private Long technicianID;

    /** Codice stabile derivato dall'ID (es. {@code TC-000042}) per ricerche UI. */
    private String technicianCode;

    private String name;
    private String email;
    private String specialization;

    /** Data/ora registrazione account. */
    private LocalDateTime registeredAt;

    /** Versione ottimistica ({@code User.version}) per aggiornamenti concorrenti. */
    private Long profileVersion;

    /** Spazi dell'host corrente a cui il tecnico è assegnato (solo contesto lista host). */
    private List<TechnicianAssignedSpaceDTO> assignedSpaces = new ArrayList<>();

}

