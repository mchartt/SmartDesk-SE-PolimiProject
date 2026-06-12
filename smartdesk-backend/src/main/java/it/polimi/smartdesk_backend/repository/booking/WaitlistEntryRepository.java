package it.polimi.smartdesk_backend.repository.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.booking.WaitlistEntry;

/** Iscrizioni waitlist per desk/giorno; ordinamento FIFO tra {@code notified=false}. */
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByWorkerIDAndDeskIDAndBookedDay(Long workerID, Long deskID, LocalDate bookedDay);

    List<WaitlistEntry> findByDeskIDAndBookedDayAndNotifiedFalseOrderByCreatedAtAsc(Long deskID, LocalDate bookedDay);

    Optional<WaitlistEntry> findByWorkerIDAndDeskIDAndBookedDay(Long workerID, Long deskID, LocalDate bookedDay);
}

