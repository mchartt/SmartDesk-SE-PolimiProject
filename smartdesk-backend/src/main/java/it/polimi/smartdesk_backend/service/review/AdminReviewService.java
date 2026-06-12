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
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.ReviewResponseMapper;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;

/** Moderazione recensioni lato sys admin: lettura per spazio, risposta ufficiale, cancellazione. L'eviction della cache medie avviene in cancellazione. */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;
    private final ReviewResponseMapper reviewResponseMapper;

    /** Elenca le recensioni di uno spazio arricchite per la console admin. */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewResponsesForSpaceForAdmin(Long spaceID) {
        List<Review> reviews = reviewRepo.findBySpaceID(spaceID);
        if (reviews.isEmpty()) {
            return List.of();
        }
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

    /**
     * Rimozione fisica della recensione; invalida la cache delle medie per spazio.
     *
     * @throws NotFoundException recensione assente
     */
    @Transactional
    public void deleteReviewAsAdmin(Long reviewID) {
        Review review = reviewRepo.findById(reviewID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.reviewNotFound(reviewID)));
        reviewRepo.delete(review);
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
                .collect(Collectors.toMap(space -> space.getSpaceID(), Function.identity()));
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
}

