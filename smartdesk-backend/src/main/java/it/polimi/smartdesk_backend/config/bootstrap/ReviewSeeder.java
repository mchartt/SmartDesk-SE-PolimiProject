package it.polimi.smartdesk_backend.config.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Inserisce recensioni demo dal JSON di seed (opzionale risposta host). */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSeeder {

    private final UserRepository users;
    private final SpaceRepository spaces;
    private final DeskRepository desks;
    private final BookingRepository bookings;
    private final ReviewRepository reviews;
    private final BookingSeeder bookingSeeder;

    /** Inserisce recensioni demo legate a prenotazioni concluse di recente. */
    public void seedReviews(List<SeedData.ReviewJson> rows) {
        if (rows == null) return;
        for (SeedData.ReviewJson row : rows) {
            if (row.getDaysSinceEnd() == null || row.getDaysSinceEnd() < 1) continue;
            var worker = users.findByEmail(row.getWorkerEmail());
            var space = spaces.findAll().stream().filter(s -> s.getName().equals(row.getSpaceName())).findFirst();
            if (worker.isEmpty() || space.isEmpty()) continue;
            
            Desk desk = desks.findBySpace_SpaceIDAndCode(space.get().getSpaceID(), row.getDeskCode()).orElse(null);
            if (desk == null) continue;

            Booking booking = findOrCreateCompletedBooking(worker.get().getId(), desk, row.getDaysSinceEnd(), row.getWorkerEmail());
            if (booking == null || reviews.existsByBookingID(booking.getBookingID())) continue;

            Review review = new Review();
            review.setWorkerID(worker.get().getId());
            review.setBookingID(booking.getBookingID());
            review.setHostID(space.get().getHostID());
            review.setSpaceID(space.get().getSpaceID());
            review.setRating(row.getRating());
            review.setComment(row.getComment());
            LocalDate createdAt = booking.getEndTime().toLocalDate().plusDays(1);
            review.setCreatedAt(createdAt);


            reviews.save(review);
            log.info("Review creata per {} su {} / {}", row.getWorkerEmail(), row.getSpaceName(), row.getDeskCode());
        }
    }

    private Booking findOrCreateCompletedBooking(Long workerId, Desk desk, int daysSinceEnd, String label) {
        LocalDateTime end = LocalDateTime.now().minusDays(daysSinceEnd).withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDate bookedDay = end.toLocalDate();
        Optional<Booking> existing = bookings.findByWorkerID(workerId).stream()
                .filter(b -> desk.getDeskID().equals(b.getDeskID()) && bookedDay.equals(b.getBookedDay()) && BookingStatus.CONFIRMED.name().equals(b.getStatus()))
                .findFirst();
        if (existing.isPresent()) return existing.get();
        bookingSeeder.createBookingIfFree(workerId, desk, end.minusHours(9), end, BookingStatus.CONFIRMED.name(), label);
        return bookings.findByWorkerID(workerId).stream()
                .filter(b -> desk.getDeskID().equals(b.getDeskID()) && bookedDay.equals(b.getBookedDay()))
                .findFirst().orElse(null);
    }
}
