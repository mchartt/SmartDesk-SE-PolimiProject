package it.polimi.smartdesk_backend.service.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.review.ReviewDTO;
import it.polimi.smartdesk_backend.dto.review.WorkerReviewHistoryDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.ReviewMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;

/** Recensioni lato worker: creazione dopo prenotazione idonea, modifica, storico. Ogni scrittura invalida la cache delle medie per spazio. */
@Service
@RequiredArgsConstructor
public class WorkerReviewService {

    private final ReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final ReviewNotificationService notificationService;

    /**
     * Persiste la recensione del worker e notifica l'host; una sola recensione per prenotazione.
     *
     * @throws BusinessRuleException prenotazione non idonea o recensione già presente
     * @throws ForbiddenException prenotazione di altro worker
     * @throws NotFoundException prenotazione/desk assenti
     */
    @Transactional
    public Review leaveReview(Long workerID, ReviewDTO reviewDTO) {
        Booking booking = loadReviewableBooking(workerID, reviewDTO);
        Desk desk = deskRepo.findById(booking.getDeskID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(booking.getDeskID())));
        if (desk.getSpace() == null) {
            throw new BusinessRuleException(SpaceMessage.deskWithoutSpace(booking.getDeskID()));
        }

        Review review = new Review();
        review.setWorkerID(workerID);
        review.setBookingID(booking.getBookingID());
        review.setHostID(desk.getSpace().getHostID());
        review.setSpaceID(desk.getSpace().getSpaceID());
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());
        review.setCreatedAt(LocalDate.now());

        Review saved = reviewRepo.save(review);
        notificationService.notifyHostOfNewReview(saved, desk.getSpace(), workerID);
        return saved;
    }

    /** Persiste la recensione e restituisce la voce nello storico del worker. */
    @Transactional
    public WorkerReviewHistoryDTO leaveReviewForWorker(Long workerID, ReviewDTO reviewDTO) {
        Review saved = leaveReview(workerID, reviewDTO);
        return toWorkerHistoryDto(saved);
    }

    /**
     * Aggiorna voto e commento di una recensione del worker.
     *
     * @throws ForbiddenException recensione di altro worker
     */
    @Transactional
    public Review updateReview(Long workerID, Long reviewID, ReviewDTO reviewDTO) {
        return doUpdateReview(workerID, reviewID, reviewDTO);
    }

    /** Aggiorna la recensione e restituisce la voce aggiornata nello storico del worker. */
    @Transactional
    public WorkerReviewHistoryDTO updateReviewForWorker(Long workerID, Long reviewID, ReviewDTO reviewDTO) {
        Review saved = doUpdateReview(workerID, reviewID, reviewDTO);
        return toWorkerHistoryDto(saved);
    }

    private Review doUpdateReview(Long workerID, Long reviewID, ReviewDTO reviewDTO) {
        Review review = loadOwnedReview(workerID, reviewID);
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());
        return reviewRepo.save(review);
    }

    /** Elimina una recensione del worker. */
    @Transactional
    public void deleteReviewAsWorker(Long workerID, Long reviewID) {
        reviewRepo.delete(loadOwnedReview(workerID, reviewID));
    }

    /** Storico completo del worker, più recenti prima. */
    @Transactional(readOnly = true)
    public List<WorkerReviewHistoryDTO> getWorkerReviewHistory(Long workerID) {
        List<Review> reviews = reviewRepo.findByWorkerIDOrderByCreatedAtDesc(workerID);
        Map<Long, Space> spacesById = loadSpaces(reviews);
        Map<Long, String> bookingCodesById = loadBookingCodes(reviews);
        return reviews.stream()
                .map(review -> toWorkerHistoryDto(
                        review,
                        spacesById.get(review.getSpaceID()),
                        bookingCodesById.get(review.getBookingID())))
                .toList();
    }

    /** Ricarica la recensione dal database e la mappa per la risposta API. */
    @Transactional(readOnly = true)
    public WorkerReviewHistoryDTO toWorkerResponse(Review review) {
        if (review == null || review.getReviewID() == null) {
            throw new NotFoundException(ResourceMessage.reviewNotFound(review == null ? null : review.getReviewID()));
        }
        Review managed = reviewRepo.findById(review.getReviewID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.reviewNotFound(review.getReviewID())));
        return toWorkerHistoryDto(managed);
    }



    private WorkerReviewHistoryDTO toWorkerHistoryDto(Review review) {
        Space space = review.getSpaceID() == null ? null : spaceRepo.findById(review.getSpaceID()).orElse(null);
        String bookingCode = review.getBookingID() == null
                ? null
                : bookingRepo.findById(review.getBookingID()).map(Booking::getBookingCode).orElse(null);
        return toWorkerHistoryDto(review, space, bookingCode);
    }

    private WorkerReviewHistoryDTO toWorkerHistoryDto(Review review, Space space, String bookingCode) {
        WorkerReviewHistoryDTO dto = new WorkerReviewHistoryDTO();
        dto.setReviewID(review.getReviewID());
        dto.setBookingID(review.getBookingID());
        dto.setBookingCode(bookingCode);
        dto.setWorkerID(review.getWorkerID());
        dto.setHostID(review.getHostID());
        dto.setSpaceID(review.getSpaceID());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        if (space != null) {
            dto.setSpaceName(space.getName());
            dto.setCity(space.getCity());
            dto.setSpaceOfficeCode(space.getOfficeCode());
        }
        return dto;
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

    private Map<Long, String> loadBookingCodes(List<Review> reviews) {
        HashSet<Long> ids = reviews.stream()
                .map(Review::getBookingID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return bookingRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Booking::getBookingID, WorkerReviewService::bookingDisplayRef));
    }

    private static String bookingDisplayRef(Booking booking) {
        String code = booking.getBookingCode();
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        return String.valueOf(booking.getBookingID());
    }

    private Booking loadReviewableBooking(Long workerID, ReviewDTO reviewDTO) {
        if (reviewDTO.getBookingID() == null) {
            throw new BusinessRuleException(ReviewMessage.REVIEW_BOOKING_ID_REQUIRED.text());
        }
        Booking booking = bookingRepo.findById(reviewDTO.getBookingID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(reviewDTO.getBookingID())));
        if (!workerID.equals(booking.getWorkerID())) {
            throw new ForbiddenException(ReviewMessage.REVIEW_BOOKING_NOT_YOURS.text());
        }
        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            throw new BusinessRuleException(ReviewMessage.REVIEW_BOOKING_MUST_BE_CONFIRMED.text());
        }
        if (!booking.isEligibleForReview(LocalDateTime.now())) {
            throw new BusinessRuleException(ReviewMessage.reviewEligibilityExpired(Booking.REVIEW_ELIGIBILITY_DAYS));
        }
        if (reviewRepo.existsByBookingID(booking.getBookingID())) {
            throw new BusinessRuleException(ReviewMessage.reviewAlreadyExists(booking.getBookingID()));
        }
        return booking;
    }

    private Review loadOwnedReview(Long workerID, Long reviewID) {
        Review review = reviewRepo.findById(reviewID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.reviewNotFound(reviewID)));
        if (!workerID.equals(review.getWorkerID())) {
            throw new ForbiddenException(ReviewMessage.REVIEW_NOT_OWNED_BY_WORKER.text());
        }
        return review;
    }
}

