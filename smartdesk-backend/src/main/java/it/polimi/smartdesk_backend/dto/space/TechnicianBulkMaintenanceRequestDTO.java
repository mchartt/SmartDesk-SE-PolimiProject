package it.polimi.smartdesk_backend.dto.space;

import java.util.List;
import jakarta.validation.constraints.NotNull;

/** Operazione bulk tecnico: lista desk da mettere in manutenzione e lista da ripristinare. */
public record TechnicianBulkMaintenanceRequestDTO(
        @NotNull List<Long> suspendIds,
        @NotNull List<Long> revertIds
) {}
