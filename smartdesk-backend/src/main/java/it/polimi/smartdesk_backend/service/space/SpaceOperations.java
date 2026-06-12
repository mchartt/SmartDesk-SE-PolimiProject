package it.polimi.smartdesk_backend.service.space;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import java.util.List;

/** Operazioni pubbliche (lato worker) sugli spazi. */
public interface SpaceOperations {
    /** Elenca solo gli spazi approvati visibili nel catalogo worker. */
    List<SpaceDTO> findAll();

    /**
     * Dettaglio spazio approvato per il catalogo worker.
     *
     * @throws it.polimi.smartdesk_backend.exception.NotFoundException spazio assente o non approvato
     */
    SpaceDTO findById(Long spaceId);

    /** Spazi del host; include non approvati (uso interno host). */
    List<SpaceDTO> findByHost(Long hostId);
}

