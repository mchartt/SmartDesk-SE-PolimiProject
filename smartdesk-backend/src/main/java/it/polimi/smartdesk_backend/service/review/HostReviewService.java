package it.polimi.smartdesk_backend.service.review;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.ReviewResponseMapper;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.ReviewMessage;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import lombok.RequiredArgsConstructor;

/** Flusso recensioni per l'host: lettura, segna come vista, nota/risposta (max 200 caratteri), notifica al worker solo alla prima risposta non vuota. */
@Service
@RequiredArgsConstructor
public class HostReviewService {

    private final ReviewRepository reviewRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;
    private final ReviewResponseMapper reviewResponseMapper;
    private final HostOwnershipService hostOwnershipService;
    private final ReviewNotificationService notificationService;

    /** Elenca tutte le recensioni ricevute dall'host. */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewResponsesForHost(Long hostID) {
        List<Review> reviews = reviewRepo.findByHostID(hostID);
        return toEnrichedDtos(reviews);
    }

    /** Elenca le recensioni di uno spazio dopo verifica di ownership host. */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewResponsesForSpace(Long spaceID) {
        List<Review> reviews = reviewRepo.findBySpaceID(spaceID);
        return toEnrichedDtos(reviews);
    }

    /**
     * Contrassegna una recensione come vista dall'host.
     *
     * @throws NotFoundException recensione assente
     * @throws ForbiddenException recensione di altro spazio
     */
    @Transactional
    public Review markReviewSeenByHost(Long hostUserId, Long spaceId, Long reviewId) {
        Review review = loadOwnedSpaceReview(hostUserId, spaceId, reviewId);
        review.setSeenByHost(true);
        return reviewRepo.save(review);
    }

    /** Contrassegna la recensione come vista e restituisce il DTO arricchito. */
    @Transactional
    public ReviewResponseDTO markReviewSeenByHostResponse(Long hostUserId, Long spaceId, Long reviewId) {
        Review saved = markReviewSeenByHost(hostUserId, spaceId, reviewId);
        return toEnrichedDto(saved);
    }



    private List<ReviewResponseDTO> toEnrichedDtos(List<Review> reviews) {
        if (reviews.isEmpty()) return List.of();
        Map<Long, Space> spacesById = loadSpaces(reviews);
        Map<Long, User> usersById = loadWorkers(reviews);

        return reviews.stream()
                .map(review -> {
                    return reviewResponseMapper.toDto(
                            review,
                            spacesById.get(review.getSpaceID()),
                            usersById.get(review.getWorkerID())
                    );
                })
                .collect(Collectors.toList());
    }

    private ReviewResponseDTO toEnrichedDto(Review review) {
        if (review == null) return null;

        Space space = review.getSpaceID() == null ? null : spaceRepo.findById(review.getSpaceID()).orElse(null);
        User worker = review.getWorkerID() == null ? null : userRepo.findById(review.getWorkerID()).orElse(null);

        return reviewResponseMapper.toDto(review, space, worker);
    }

    private Map<Long, Space> loadSpaces(List<Review> reviews) {
        HashSet<Long> ids = reviews.stream()
                .map(Review::getSpaceID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return spaceRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Space::getSpaceID, Function.identity()));
    }

    private Map<Long, User> loadWorkers(List<Review> reviews) {
        HashSet<Long> ids = reviews.stream()
                .map(Review::getWorkerID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Review loadOwnedSpaceReview(Long hostUserId, Long spaceId, Long reviewId) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostUserId, spaceId);
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.reviewNotFound(reviewId)));
        if (!spaceId.equals(review.getSpaceID())) {
            throw new ForbiddenException(ReviewMessage.reviewNotInSpace(spaceId));
        }
        return review;
    }
}

