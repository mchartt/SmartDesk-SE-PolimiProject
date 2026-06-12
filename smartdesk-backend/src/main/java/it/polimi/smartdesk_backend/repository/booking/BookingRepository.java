package it.polimi.smartdesk_backend.repository.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.polimi.smartdesk_backend.model.booking.Booking;
/** Persistenza prenotazioni: overlap temporale, codici univoci e query per worker/desk/spazio. */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findByBookingCodeIsNull();

    List<Booking> findByDeskIDAndBookedDay(Long deskID, LocalDate bookedDay);

    List<Booking> findByDeskIDAndBookedDayAndBookingIDNot(Long deskID, LocalDate bookedDay, Long bookingID);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.deskID = :deskID
            AND b.bookedDay = :bookedDay
            AND b.status <> 'CANCELLED'
            ORDER BY b.startTime ASC
            """)
    List<Booking> findActiveByDeskAndBookedDay(@Param("deskID") Long deskID, @Param("bookedDay") LocalDate bookedDay);

    /** Prenotazioni non cancellate che intersecano il giorno {@code date}, usando startTime/endTime come fonte di verità. */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.deskID = :deskID
            AND b.status <> 'CANCELLED'
            AND b.startTime < :dayEndExclusive
            AND b.endTime > :dayStartInclusive
            ORDER BY b.startTime ASC
            """)
    List<Booking> findActiveOverlappingCalendarDay(
            @Param("deskID") Long deskID,
            @Param("dayStartInclusive") LocalDateTime dayStartInclusive,
            @Param("dayEndExclusive") LocalDateTime dayEndExclusive);

    default List<Booking> findActiveNonCancelledOverlappingDay(Long deskID, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime endExclusive = date.plusDays(1).atStartOfDay();
        return findActiveOverlappingCalendarDay(deskID, start, endExclusive);
    }

    List<Booking> findAllByOrderByStartTimeDesc();

    List<Booking> findByWorkerID(Long workerID);

    List<Booking> findByWorkerIDAndDeskID(Long workerID, Long deskID);

    List<Booking> findByDeskIDAndStatus(Long deskID, String status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.deskID = :deskID
            AND b.status <> 'CANCELLED'
            AND b.endTime > :now
            ORDER BY b.startTime ASC
            """)
    List<Booking> findActiveFutureByDesk(@Param("deskID") Long deskID, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.deskID = :deskId
            AND b.status <> 'CANCELLED'
            AND b.startTime < :end
            AND b.endTime > :start
            AND (:excludeId IS NULL OR b.bookingID <> :excludeId)
            """)
    long countDeskOverlapping(
            @Param("deskId") Long deskId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeBookingId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.workerID = :workerId
            AND b.status <> 'CANCELLED'
            AND b.startTime < :end
            AND b.endTime > :start
            AND (:excludeId IS NULL OR b.bookingID <> :excludeId)
            """)
    long countWorkerOverlapping(
            @Param("workerId") Long workerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeBookingId);

    /** Prenotazioni non cancellate nello spazio i cui giorni cadono nei giorni indicati. */
    @Query("""
            SELECT b FROM Booking b, Desk d
            WHERE b.deskID = d.deskID
            AND d.space.spaceID = :spaceId
            AND b.bookedDay IN :days
            AND b.status <> 'CANCELLED'
            """)
    List<Booking> findActiveForSpaceOnBookedDays(
            @Param("spaceId") Long spaceId,
            @Param("days") Collection<LocalDate> days);

    @Query("""
            SELECT b FROM Booking b, Desk d
            WHERE b.deskID = d.deskID
            AND d.space.hostID = :hostId
            AND b.status <> 'CANCELLED'
            ORDER BY b.startTime DESC
            """)
    List<Booking> findAllByDeskHost(@Param("hostId") Long hostId);

    int deleteByBookingIDAndStatusNot(Long bookingID, String status);
}

