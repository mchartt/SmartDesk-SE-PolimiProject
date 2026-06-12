package it.polimi.smartdesk_backend.service.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.repository.review.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewStatsServiceTest {

    @Mock
    private ReviewRepository reviewRepo;

    @InjectMocks
    private ReviewStatsService reviewStatsService;

    @Test
    void shouldBuildAverageRatingBySpaceId() {
        when(reviewRepo.findAverageRatingBySpaceId()).thenReturn(List.of(
                new Object[] { 10L, 4.5 },
                new Object[] { 11L, 3.0 }));

        Map<Long, Double> averages = reviewStatsService.averageRatingBySpaceId();

        assertEquals(2, averages.size());
        assertEquals(4.5, averages.get(10L));
        assertEquals(3.0, averages.get(11L));
    }

    @Test
    void shouldReturnEmptyMapWhenNoReviews() {
        when(reviewRepo.findAverageRatingBySpaceId()).thenReturn(List.of());

        assertEquals(Map.of(), reviewStatsService.averageRatingBySpaceId());
    }
}
