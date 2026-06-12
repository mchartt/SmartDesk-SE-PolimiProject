package it.polimi.smartdesk_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.User;

/** Mapping recensione → {@link ReviewResponseDTO} con dati spazio e worker denormalizzati. */
@Mapper(componentModel = "spring")
public interface ReviewResponseMapper {

    /** Mappa la recensione con spazio e worker già caricati. */
    @Mapping(target = "hostID", source = "review.hostID")
    @Mapping(target = "spaceID", source = "review.spaceID")
    @Mapping(target = "spaceOfficeCode", source = "space.officeCode")
    @Mapping(target = "spaceName", source = "space.name")
    @Mapping(target = "city", source = "space.city")
    @Mapping(target = "workerGivenName", source = "worker.name")
    @Mapping(target = "workerFamilyName", source = "worker.surname")
    @Mapping(target = "workerEmail", source = "worker.email")
    ReviewResponseDTO toDto(Review review, Space space, User worker);

    /** Variante senza join: campi spazio/worker restano vuoti. */
    default ReviewResponseDTO toDto(Review review) {
        return toDto(review, null, null);
    }
}
