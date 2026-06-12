package it.polimi.smartdesk_backend.mapper;

import java.util.ArrayList;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import it.polimi.smartdesk_backend.dto.space.SpaceAmenityPresetDTO;
import it.polimi.smartdesk_backend.model.space.SpaceAmenityPreset;

/** Mapping preset amenity; {@code amenities} clonata in {@link #copyAmenities} per non esporre la collection JPA. */
@Mapper(componentModel = "spring")
public interface SpaceAmenityPresetMapper {

    /** Converte il preset copiando la lista amenity per isolarla dalla collection JPA. */
    @Mapping(target = "spaceID", source = "space.spaceID")
    SpaceAmenityPresetDTO toDto(SpaceAmenityPreset preset);

    /** Copia difensiva delle amenity nel DTO di risposta. */
    @AfterMapping
    default void copyAmenities(SpaceAmenityPreset preset, @MappingTarget SpaceAmenityPresetDTO dto) {
        dto.setAmenities(new ArrayList<>(preset.getAmenities()));
    }
}

