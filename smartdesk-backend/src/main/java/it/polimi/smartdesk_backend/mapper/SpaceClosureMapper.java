package it.polimi.smartdesk_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import it.polimi.smartdesk_backend.dto.space.SpaceClosureDTO;
import it.polimi.smartdesk_backend.model.space.SpaceClosure;

/** Giorni di chiusura straordinaria della sede, esposti in API con lo {@code spaceID} denormalizzato. */
@Mapper(componentModel = "spring")
public interface SpaceClosureMapper {

    /** Mappa la chiusura con {@code spaceID} denormalizzato. */
    @Mapping(target = "spaceID", source = "space.spaceID")
    SpaceClosureDTO toDto(SpaceClosure closure);
}

