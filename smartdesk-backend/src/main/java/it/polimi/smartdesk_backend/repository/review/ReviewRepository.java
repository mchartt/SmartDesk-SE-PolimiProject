package it.polimi.smartdesk_backend.repository.review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.polimi.smartdesk_backend.model.review.Review;

/** Recensioni per spazio/host/booking e aggregazione media voto per {@code spaceID}. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r.spaceID, AVG(r.rating) FROM Review r GROUP BY r.spaceID")
    List<Object[]> findAverageRatingBySpaceId();

    List<Review> findByHostID(Long hostID);

    List<Review> findBySpaceID(Long spaceID);

    Optional<Review> findByBookingID(Long bookingID);

    boolean existsByBookingID(Long bookingID);

    List<Review> findByWorkerIDOrderByCreatedAtDesc(Long workerID);

    List<Review> findByBookingIDIn(Collection<Long> bookingIDs);
}

