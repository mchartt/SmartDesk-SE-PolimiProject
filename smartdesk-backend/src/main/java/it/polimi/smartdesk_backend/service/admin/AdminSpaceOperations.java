package it.polimi.smartdesk_backend.service.admin;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import java.util.List;

/** Operazioni amministrative sugli spazi. */
public interface AdminSpaceOperations {
    /** Tutti gli spazi senza filtro approvazione. */
    List<SpaceDTO> findAllForAdmin();

    /** Spazi approvati con media recensioni e dati host. */
    List<SpaceDTO> findApprovedEnrichedForAdmin();

    /** Coda approvazione: {@code approved=false}. */
    List<SpaceDTO> findPendingApprovalForAdmin();
}

