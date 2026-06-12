package it.polimi.smartdesk_backend.mapper;

import org.mapstruct.Mapper;

import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.model.space.Space;

/** Proiezione minimale di {@link Space} per l'elenco sedi assegnate al tecnico. */
@Mapper(componentModel = "spring")
public interface TechnicianAssignedSpaceMapper {

    /** Mappa id, nome e codice ufficio dello spazio. */
    TechnicianAssignedSpaceDTO toDto(Space space);
}

