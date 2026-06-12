package it.polimi.smartdesk_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;

/** Mapping spazio → {@link SpaceDTO}; rating, host e orari sono arricchiti nel service. */
@Mapper(componentModel = "spring")
public interface SpaceMapper {

    /** Mappatura base; rating e dati host sono ignorati e valorizzati nel service. */
    @Mapping(target = "deskCount", expression = "java(space.getDesks() != null ? space.getDesks().size() : 0)")
    @Mapping(target = "averageReviewRating", ignore = true)
    @Mapping(target = "hostName", ignore = true)
    @Mapping(target = "hostEmail", ignore = true)
    @Mapping(target = "hostGivenName", ignore = true)
    @Mapping(target = "hostFamilyName", ignore = true)
    @Mapping(target = "hostVatNumber", ignore = true)
    @Mapping(target = "openingHours", ignore = true)
    SpaceDTO toDto(Space space);

    /** Arricchisce il DTO con i campi anagrafici dell'host, se presente. */
    default void applyHostFields(SpaceDTO dto, Host host) {
        if (host == null) return;
        dto.setHostGivenName(trimOrNull(host.getName()));
        dto.setHostFamilyName(trimOrNull(host.getSurname()));
        dto.setHostEmail(trimOrNull(host.getEmail()));
        dto.setHostName(buildHostShortLabel(host));
        dto.setHostVatNumber(trimOrNull(host.getVatNumber()));
    }

    /** Nome e cognome host in una stringa; null se entrambi vuoti. */
    private String buildHostShortLabel(Host host) {
        String givenName = host.getName() == null ? "" : host.getName().trim();
        String familyName = host.getSurname() == null ? "" : host.getSurname().trim();
        String joined = (givenName + " " + familyName).trim();
        return joined.isEmpty() ? null : joined;
    }

    /** Trim o null se blank. */
    private String trimOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
