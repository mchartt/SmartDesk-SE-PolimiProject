package it.polimi.smartdesk_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import it.polimi.smartdesk_backend.dto.space.RoomDTO;
import it.polimi.smartdesk_backend.model.space.Room;

/** MapStruct per le stanze: il {@code spaceID} viene estratto dalla relazione, non duplicato a mano. */
@Mapper(componentModel = "spring")
public interface RoomMapper {

    /** Mappa la stanza; {@code spaceID} è derivato dalla relazione padre. */
    @Mapping(target = "spaceID", source = "space.spaceID")
    RoomDTO toDto(Room room);
}

