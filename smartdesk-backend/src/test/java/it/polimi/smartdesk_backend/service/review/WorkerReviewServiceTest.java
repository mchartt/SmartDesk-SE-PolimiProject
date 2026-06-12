package it.polimi.smartdesk_backend.service.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.review.ReviewDTO;
import it.polimi.smartdesk_backend.dto.review.WorkerReviewHistoryDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;

@ExtendWith(MockitoExtension.class)
class WorkerReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private DeskRepository deskRepo;
    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private ReviewNotificationService notificationService;

    @InjectMocks
    private WorkerReviewService workerReviewService;

    @Test
    void shouldLeaveReviewAndNotifyHost() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookingID(10L);
        dto.setRating(5);
        dto.setComment("Ottima esperienza, postazioni pulite e host molto disponibile.");

        Booking booking = new Booking();
        booking.setBookingID(10L);
        booking.setWorkerID(7L);
        booking.setDeskID(3L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setEndTime(LocalDateTime.now().minusDays(1));

        Space space = new Space();
        space.setSpaceID(2L);
        space.setHostID(9L);
        space.setName("Space A");

        Desk desk = new Desk();
        desk.setDeskID(3L);
        desk.setSpace(space);

        Review saved = new Review();
        saved.setReviewID(55L);
        saved.setBookingID(10L);
        saved.setWorkerID(7L);
        saved.setHostID(9L);
        saved.setSpaceID(2L);
        saved.setComment(dto.getComment());
        saved.setRating(5);

        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepo.existsByBookingID(10L)).thenReturn(false);
        when(deskRepo.findById(3L)).thenReturn(Optional.of(desk));
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);

        Review result = workerReviewService.leaveReview(7L, dto);

        assertEquals(55L, result.getReviewID());
        assertEquals(9L, result.getHostID());
        verify(notificationService).notifyHostOfNewReview(eq(saved), eq(space), eq(7L));
    }

    @Test
    void shouldRejectReviewForAnotherWorkerBooking() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookingID(10L);

        Booking booking = new Booking();
        booking.setBookingID(10L);
        booking.setWorkerID(99L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setEndTime(LocalDateTime.now().minusHours(1));

        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> workerReviewService.leaveReview(7L, dto));
    }

    @Test
    void shouldRejectDuplicateReviewForBooking() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookingID(10L);

        Booking booking = new Booking();
        booking.setBookingID(10L);
        booking.setWorkerID(7L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setEndTime(LocalDateTime.now().minusHours(2));

        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepo.existsByBookingID(10L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> workerReviewService.leaveReview(7L, dto));
    }

    @Test
    void shouldReturnWorkerReviewHistoryWithMappedFields() {
        Review review = new Review();
        review.setReviewID(1L);
        review.setBookingID(10L);
        review.setWorkerID(7L);
        review.setHostID(9L);
        review.setSpaceID(2L);
        review.setRating(4);
        review.setComment("Ottima esperienza, postazioni pulite e host molto disponibile.");

        Space space = new Space();
        space.setSpaceID(2L);
        space.setName("Space A");
        space.setCity("Milano");
        space.setOfficeCode("MI001");

        Booking booking = new Booking();
        booking.setBookingID(10L);
        booking.setBookingCode("ABC123");

        when(reviewRepo.findByWorkerIDOrderByCreatedAtDesc(7L)).thenReturn(List.of(review));
        when(spaceRepo.findAllById(any())).thenReturn(List.of(space));
        when(bookingRepo.findAllById(any())).thenReturn(List.of(booking));

        List<WorkerReviewHistoryDTO> result = workerReviewService.getWorkerReviewHistory(7L);

        assertEquals(1, result.size());
        assertEquals("Space A", result.get(0).getSpaceName());
        assertEquals("ABC123", result.get(0).getBookingCode());
    }

    @Test
    void shouldUpdateOwnedReview() {
        ReviewDTO dto = new ReviewDTO();
        dto.setRating(3);
        dto.setComment("Commento aggiornato con lunghezza sufficiente per le regole di validazione.");

        Review review = new Review();
        review.setReviewID(5L);
        review.setWorkerID(7L);

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepo.save(review)).thenReturn(review);

        Review saved = workerReviewService.updateReview(7L, 5L, dto);

        assertEquals(3, saved.getRating());
        assertEquals(dto.getComment(), saved.getComment());
    }

    @Test
    void shouldDeleteOwnedReview() {
        Review review = new Review();
        review.setReviewID(5L);
        review.setWorkerID(7L);

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(review));

        workerReviewService.deleteReviewAsWorker(7L, 5L);

        verify(reviewRepo).delete(review);
    }

    @Test
    void shouldLeaveReviewForWorkerAsDto() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookingID(10L);
        dto.setRating(5);
        dto.setComment("Ottima esperienza, postazioni pulite e host molto disponibile.");

        Booking booking = new Booking();
        booking.setBookingID(10L);
        booking.setWorkerID(7L);
        booking.setDeskID(3L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setEndTime(LocalDateTime.now().minusDays(1));

        Space space = new Space();
        space.setSpaceID(2L);
        space.setHostID(9L);
        Desk desk = new Desk();
        desk.setDeskID(3L);
        desk.setSpace(space);

        Review saved = new Review();
        saved.setReviewID(55L);
        saved.setWorkerID(7L);
        saved.setSpaceID(2L);

        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepo.existsByBookingID(10L)).thenReturn(false);
        when(deskRepo.findById(3L)).thenReturn(Optional.of(desk));
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);
        when(spaceRepo.findById(2L)).thenReturn(Optional.of(space));
        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));

        WorkerReviewHistoryDTO history = workerReviewService.leaveReviewForWorker(7L, dto);

        assertEquals(55L, history.getReviewID());
    }

    @Test
    void shouldUpdateReviewForWorkerAsHistoryDto() {
        ReviewDTO dto = new ReviewDTO();
        dto.setRating(4);
        dto.setComment("Aggiornato tramite API worker con caratteri sufficienti.");

        Review review = new Review();
        review.setReviewID(5L);
        review.setWorkerID(7L);
        review.setSpaceID(2L);

        Space space = new Space();
        space.setSpaceID(2L);
        space.setName("Hub");

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepo.save(review)).thenReturn(review);
        when(spaceRepo.findById(2L)).thenReturn(Optional.of(space));

        WorkerReviewHistoryDTO history = workerReviewService.updateReviewForWorker(7L, 5L, dto);

        assertEquals(5L, history.getReviewID());
        assertEquals(4, history.getRating());
    }

    @Test
    void shouldMapReviewToWorkerResponse() {
        Review review = new Review();
        review.setReviewID(5L);
        review.setWorkerID(7L);
        review.setSpaceID(2L);

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(review));
        when(spaceRepo.findById(2L)).thenReturn(Optional.of(new Space()));

        WorkerReviewHistoryDTO history = workerReviewService.toWorkerResponse(review);

        assertEquals(5L, history.getReviewID());
    }
}
