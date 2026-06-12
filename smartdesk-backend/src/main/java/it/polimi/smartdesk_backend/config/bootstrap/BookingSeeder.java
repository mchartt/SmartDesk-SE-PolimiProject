package it.polimi.smartdesk_backend.config.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.support.codegen.CodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Riempie qualche prenotazione di esempio sui desk già creati. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSeeder {

    private final UserRepository users;
    private final SpaceRepository spaces;
    private final DeskRepository desks;
    private final BookingRepository bookings;

    /** Crea prenotazioni demo da righe JSON (orari assoluti o relativi a oggi). */
    public void seedBookings(List<SeedData.BookingJson> rows) {
        if (rows == null) return;
        for (SeedData.BookingJson row : rows) {
            var worker = users.findByEmail(row.getWorkerEmail());
            var space = spaces.findAll().stream().filter(s -> s.getName().equals(row.getSpaceName())).findFirst();
            if (worker.isEmpty() || space.isEmpty()) continue;

            Desk desk = desks.findBySpace_SpaceIDAndCode(space.get().getSpaceID(), row.getDeskCode()).orElse(null);
            if (desk == null) {
                log.warn("Desk {} non trovato nello spazio {}", row.getDeskCode(), row.getSpaceName());
                continue;
            }
            if (DeskStateCode.MAINTENANCE.equals(desk.getStateCode())) {
                log.warn("Desk {} in spazio {} saltato (in manutenzione)", row.getDeskCode(), row.getSpaceName());
                continue;
            }

            LocalDateTime start;
            LocalDateTime end;
            if (row.getDaysFromNow() != null && row.getStartHour() != null && row.getEndHour() != null) {
                LocalDate day = LocalDate.now().plusDays(row.getDaysFromNow());
                start = day.atTime(row.getStartHour(), 0);
                end = day.atTime(row.getEndHour(), 0);
            } else if (row.getStartTime() != null && row.getEndTime() != null) {
                start = LocalDateTime.parse(row.getStartTime());
                end = LocalDateTime.parse(row.getEndTime());
            } else continue;

            if (!start.toLocalDate().equals(end.toLocalDate()) || !end.isAfter(start)) continue;

            createBookingIfFree(worker.get().getId(), desk, start, end, row.getStatus(), row.getWorkerEmail());
        }
    }

    /** Due prenotazioni concluse di recente, utili per provare le recensioni. */
    public void seedRecentCompletedBookingsForReviews() {
        createPastBookingIfFree("mario.rossi@worker.com", "Milano Central Coworking", "B2", 3);
        createPastBookingIfFree("anna.bianchi@worker.com", "Torino Tech Space", "P1", 2);
    }

    /** Prenotazione passata su desk libero, se worker e spazio esistono nel seed. */
    public void createPastBookingIfFree(String workerEmail, String spaceName, String deskCode, int daysSinceEnd) {
        var worker = users.findByEmail(workerEmail);
        var space = spaces.findAll().stream().filter(s -> s.getName().equals(spaceName)).findFirst();
        if (worker.isEmpty() || space.isEmpty()) return;
        
        desks.findBySpace_SpaceIDAndCode(space.get().getSpaceID(), deskCode).ifPresent(desk -> {
            LocalDateTime end = LocalDateTime.now().minusDays(daysSinceEnd).withHour(18).withMinute(0).withSecond(0).withNano(0);
            createBookingIfFree(worker.get().getId(), desk, end.minusHours(9), end, BookingStatus.CONFIRMED.name(), workerEmail);
        });
    }

    /** Salva una prenotazione solo se il desk non ne ha già una nello stesso giorno. */
    public void createBookingIfFree(Long workerId, Desk desk, LocalDateTime start, LocalDateTime end, String status, String label) {
        LocalDate day = start.toLocalDate();
        if (!bookings.findByDeskIDAndBookedDay(desk.getDeskID(), day).isEmpty()) return;

        Booking booking = new Booking();
        booking.setWorkerID(workerId);
        booking.setDeskID(desk.getDeskID());
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setBookedDay(day);
        booking.setStatus(status);
        booking.setBookingCode(CodeUtils.allocateUniqueCode(bookings::existsByBookingCode, 64, "BOOKING_CODE"));
        bookings.save(booking);
        log.info("Booking creato per: {}", label);
    }
}
