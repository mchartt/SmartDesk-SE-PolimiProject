package it.polimi.smartdesk_backend.mapper;

import java.util.ArrayList;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.model.user.Technician;

/** Mapping tecnico → {@link TechnicianDTO}: codice TC, versione profilo e spazi assegnati. */
@Mapper(componentModel = "spring")
public interface TechnicianMapper {

    /** Mappa i campi base del tecnico; gli spazi assegnati sono valorizzati dal service. */
    @Mapping(target = "technicianID", source = "id")
    @Mapping(target = "technicianCode", expression = "java(formatTechnicianCode(technician.getId()))")
    @Mapping(target = "profileVersion", source = "version")
    TechnicianDTO toDto(Technician technician);

    /** {@code null} se l’id non c’è; altrimenti prefisso {@code TC-} con padding a sei cifre. */
    default String formatTechnicianCode(Long technicianId) {
        if (technicianId == null) {
            return null;
        }
        return String.format("TC-%06d", technicianId);
    }

    /** Inizializza la lista spazi assegnati vuota; il service la popola dopo il mapping. */
    @AfterMapping
    default void resetAssignedSpaces(@MappingTarget TechnicianDTO dto) {
        dto.setAssignedSpaces(new ArrayList<>());
    }
}

