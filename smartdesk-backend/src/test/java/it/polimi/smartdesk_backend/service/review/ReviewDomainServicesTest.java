package it.polimi.smartdesk_backend.service.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.mapper.ReviewResponseMapper;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class ReviewDomainServicesTest {

    private static final Long HOST_ID = 4L;
    private static final Long SPACE_ID = 10L;
    private static final Long REVIEW_ID = 5L;

    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ReviewResponseMapper reviewResponseMapper;
    @Mock
    private HostOwnershipService hostOwnershipService;
    @Mock
    private NotificationService notificationService;

    private HostReviewService hostReviewService;
    private AdminReviewService adminReviewService;
    private ReviewNotificationService reviewNotificationService;

    @BeforeEach
    void setUp() {
        reviewNotificationService = new ReviewNotificationService(notificationService, spaceRepo, userRepo);
        hostReviewService = new HostReviewService(
                reviewRepo, spaceRepo, userRepo, reviewResponseMapper, hostOwnershipService, reviewNotificationService);
        adminReviewService = new AdminReviewService(reviewRepo, spaceRepo, userRepo, reviewResponseMapper);
    }

  // --- HostReviewService ---

    @Test
    void shouldMarkReviewAsSeenByHost() {
        Review review = ownedReview();

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space());
        when(reviewRepo.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepo.save(review)).thenReturn(review);

        Review saved = hostReviewService.markReviewSeenByHost(HOST_ID, SPACE_ID, REVIEW_ID);

        assertTrue(saved.isSeenByHost());
    }


    @Test
    void shouldListReviewResponsesForHost() {
        Review review = ownedReview();
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setReviewID(REVIEW_ID);

        when(reviewRepo.findByHostID(HOST_ID)).thenReturn(List.of(review));
        when(spaceRepo.findAllById(any())).thenReturn(List.of(space()));
        when(userRepo.findAllById(any())).thenReturn(List.of(EntityTestFixtures.worker(7L)));
        when(reviewResponseMapper.toDto(eq(review), any(), any())).thenReturn(dto);

        List<ReviewResponseDTO> result = hostReviewService.getReviewResponsesForHost(HOST_ID);

        assertEquals(1, result.size());
    }

    @Test
    void shouldListReviewResponsesForSpace() {
        Review review = ownedReview();
        ReviewResponseDTO dto = new ReviewResponseDTO();

        when(reviewRepo.findBySpaceID(SPACE_ID)).thenReturn(List.of(review));
        when(spaceRepo.findAllById(any())).thenReturn(List.of(space()));
        when(userRepo.findAllById(any())).thenReturn(List.of(EntityTestFixtures.worker(7L)));
        when(reviewResponseMapper.toDto(eq(review), any(), any())).thenReturn(dto);

        assertEquals(1, hostReviewService.getReviewResponsesForSpace(SPACE_ID).size());
    }


    @Test
    void shouldReturnDtoWhenMarkingReviewSeenByHost() {
        Review review = ownedReview();
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setReviewID(REVIEW_ID);
        Space space = space();
        Worker worker = EntityTestFixtures.worker(7L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(reviewRepo.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepo.save(review)).thenReturn(review);
        when(spaceRepo.findById(SPACE_ID)).thenReturn(Optional.of(space));
        when(userRepo.findById(7L)).thenReturn(Optional.of(worker));
        when(reviewResponseMapper.toDto(eq(review), any(), any())).thenReturn(dto);

        ReviewResponseDTO result = hostReviewService.markReviewSeenByHostResponse(HOST_ID, SPACE_ID, REVIEW_ID);

        assertEquals(REVIEW_ID, result.getReviewID());
        assertTrue(review.isSeenByHost());
    }

    @Test
    void shouldRejectReviewOutsideOwnedSpace() {
        Review review = ownedReview();
        review.setSpaceID(99L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space());
        when(reviewRepo.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThrows(ForbiddenException.class,
                () -> hostReviewService.markReviewSeenByHost(HOST_ID, SPACE_ID, REVIEW_ID));
    }

  // --- AdminReviewService ---

    @Test
    void shouldDeleteReviewAsAdmin() {
        Review review = ownedReview();
        when(reviewRepo.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        adminReviewService.deleteReviewAsAdmin(REVIEW_ID);

        verify(reviewRepo).delete(review);
    }


    @Test
    void shouldReturnEmptyAdminReviewListForSpace() {
        when(reviewRepo.findBySpaceID(SPACE_ID)).thenReturn(List.of());

        List<ReviewResponseDTO> result = adminReviewService.getReviewResponsesForSpaceForAdmin(SPACE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldMapAdminReviewListForSpace() {
        Review review = ownedReview();
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setReviewID(REVIEW_ID);
        Space space = space();
        Worker worker = EntityTestFixtures.worker(7L);

        when(reviewRepo.findBySpaceID(SPACE_ID)).thenReturn(List.of(review));
        when(spaceRepo.findAllById(any())).thenReturn(List.of(space));
        when(userRepo.findAllById(any())).thenReturn(List.of(worker));
        when(reviewResponseMapper.toDto(review, space, worker)).thenReturn(dto);

        List<ReviewResponseDTO> result = adminReviewService.getReviewResponsesForSpaceForAdmin(SPACE_ID);

        assertEquals(1, result.size());
        assertEquals(REVIEW_ID, result.get(0).getReviewID());
    }


  // --- ReviewNotificationService ---

    @Test
    void shouldNotifyHostOfNewReview() {
        Review review = ownedReview();
        Space space = space();
        Worker worker = EntityTestFixtures.worker(7L);
        worker.setName("Mario");
        worker.setSurname("Rossi");
        worker.setEmail("mario@test.it");

        when(userRepo.findById(7L)).thenReturn(Optional.of(worker));

        reviewNotificationService.notifyHostOfNewReview(review, space, 7L);

        verify(notificationService).sendWorkerActivityNotification(
                eq(HOST_ID),
                any(String.class),
                eq(ReviewNotificationService.HOST_REVIEW_LEFT),
                eq("Mario"),
                eq("Rossi"),
                eq("mario@test.it"),
                eq(5));
    }


    private Review ownedReview() {
        Review review = new Review();
        review.setReviewID(REVIEW_ID);
        review.setHostID(HOST_ID);
        review.setSpaceID(SPACE_ID);
        review.setWorkerID(7L);
        review.setRating(5);
        review.setComment("Ottima esperienza, postazioni pulite e host molto disponibile.");
        return review;
    }

    private Space space() {
        return EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
    }
}
