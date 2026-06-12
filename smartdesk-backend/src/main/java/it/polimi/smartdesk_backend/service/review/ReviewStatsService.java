package it.polimi.smartdesk_backend.service.review;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;

/** Medie voto per spazio. */
@Service
@RequiredArgsConstructor
public class ReviewStatsService {

    public static final String AVERAGE_RATING_BY_SPACE_CACHE = "averageRatingBySpaceId";

    private final ReviewRepository reviewRepo;

    /**
     * Mappa immutabile spaceId → media; spazi senza recensioni non compaiono.
     *
     * @return medie aggregate dal repository
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> averageRatingBySpaceId() {
        Map<Long, Double> avgBySpaceId = new HashMap<>();
        for (Object[] row : reviewRepo.findAverageRatingBySpaceId()) {
            Long spaceId = (Long) row[0];
            double avg = ((Number) row[1]).doubleValue();
            avgBySpaceId.put(spaceId, avg);
        }
        return Map.copyOf(avgBySpaceId);
    }
}

