package it.polimi.smartdesk_backend.service.booking;
import it.polimi.smartdesk_backend.mapper.DeskMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.booking.SearchCriteriaDTO;
import it.polimi.smartdesk_backend.dto.booking.SlotStatusDTO;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.OpeningHoursDayDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.space.OpeningHoursService;
import it.polimi.smartdesk_backend.support.TimeIntervalUtils;
import lombok.RequiredArgsConstructor;

/** Ricerca desk liberi e griglia slot dinamica basata su orari di apertura dello spazio. La disponibilità reale deriva da prenotazioni attive e chiusure spazio, non solo dallo stato macchina del desk. */
@Service
@RequiredArgsConstructor
public class DeskAvailabilityService {

    private static final DateTimeFormatter SLOT_TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final SpaceClosureRepository spaceClosureRepository;
    private final DeskStateMachine deskStateMachine;
    private final ReviewStatsService reviewStatsService;
    private final OpeningHoursService openingHoursService;

    /**
     * Filtra spazi approvati per città, spazio, amenity e finestra temporale.
     * Desk in manutenzione compaiono solo se {@code includeMaintenance} è true; incrocia overlap prenotazioni e chiusure calendario.
     *
     * @param criteria data target, eventuale finestra start/end, filtri opzionali
     * @return desk idonei alla prenotazione (o in manutenzione se richiesto)
     */
    @Transactional(readOnly = true)
    public List<DeskDTO> searchDesks(SearchCriteriaDTO criteria) {
        List<DeskDTO> availableDesks = new ArrayList<>();
        LocalDate targetDate = criteria.getTargetDate();
        LocalDateTime windowStart = criteria.getStartTime();
        LocalDateTime windowEnd = criteria.getEndTime();
        boolean useWindow = windowStart != null && windowEnd != null && windowEnd.isAfter(windowStart);

        Map<Long, Double> avgBySpace = reviewStatsService.averageRatingBySpaceId();

        for (Space space : spaceRepo.findByApprovedTrue()) {
            if (criteria.getSpaceId() != null && !criteria.getSpaceId().equals(space.getSpaceID())) {
                continue;
            }
            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
                String city = space.getCity();
                if (city == null || !city.equalsIgnoreCase(criteria.getCity())) {
                    continue;
                }
            }
            if (isSpaceClosedForDeskSearch(space.getSpaceID(), targetDate, useWindow, windowStart, windowEnd)) {
                continue;
            }
            Double spaceAvg = avgBySpace.get(space.getSpaceID());
            for (Desk desk : space.getDesks()) {
                boolean hasAmenities = desk.getAmenities().containsAll(criteria.getRequiredAmenities());
                boolean includeMaintenance = Boolean.TRUE.equals(criteria.getIncludeMaintenance());

                if (!deskStateMachine.isBookable(desk)) {
                    if (includeMaintenance && hasAmenities) {
                        availableDesks.add(DeskMapper.searchResult(
                                desk, space.getSpaceID(), spaceAvg, desk.getStateCode(), false));
                    }
                    continue;
                }

                boolean blocked;
                if (useWindow) {
                    blocked = bookingRepo.countDeskOverlapping(desk.getDeskID(), windowStart, windowEnd, null) > 0;
                } else {
                    blocked = bookingRepo.findByDeskIDAndBookedDay(desk.getDeskID(), targetDate).stream()
                            .anyMatch(b -> !BookingStatus.CANCELLED.name().equals(b.getStatus()));
                }

                if (hasAmenities && !blocked) {
                    availableDesks.add(DeskMapper.searchResult(
                            desk, space.getSpaceID(), spaceAvg, DeskStateCode.AVAILABLE, true));
                }
            }
        }
        return availableDesks;
    }

    /**
     * Griglia oraria del giorno: slot da 30 minuti sugli orari di apertura, etichetta HH:mm, stato {@code free} o {@code busy}.
     * Giorno di chiusura spazio: tutti gli slot risultano occupati.
     *
     * @param deskID desk da analizzare
     * @param date giorno calendario
     * @return slot disponibili nella finestra operativa
     * @throws NotFoundException desk inesistente
     */
    @Transactional(readOnly = true)
    public List<SlotStatusDTO> getSlotAvailability(Long deskID, LocalDate date) {
        Desk desk = deskRepo.findById(deskID).orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskID)));
        OpeningHoursDayDTO limits = openingHoursService.getLimitsForDay(desk.getSpace(), date);

        if (limits.isClosed() || (desk.getSpace() != null
                && spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(desk.getSpace().getSpaceID(), date))) {
            return generateSlotGrid(date, limits, t -> true);
        }

        List<Booking> bookings = bookingRepo.findActiveNonCancelledOverlappingDay(deskID, date);
        return generateSlotGrid(date, limits, slotStart -> {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            return bookings.stream()
                    .anyMatch(b -> TimeIntervalUtils.overlaps(slotStart, slotEnd, b.getStartTime(), b.getEndTime()));
        });
    }

    private List<SlotStatusDTO> generateSlotGrid(LocalDate date, OpeningHoursDayDTO limits,
            Predicate<LocalDateTime> busyPredicate) {
        String openStr = (limits != null && limits.getOpen() != null) ? limits.getOpen() : "09:00";
        String closeStr = (limits != null && limits.getClose() != null) ? limits.getClose() : "18:00";
        LocalTime openTime = LocalTime.parse(openStr);
        LocalTime closeTime = LocalTime.parse(closeStr);
        LocalDateTime gridStart = date.atTime(openTime);
        LocalDateTime gridEnd = date.atTime(closeTime);

        List<SlotStatusDTO> slots = new ArrayList<>();
        for (LocalDateTime slotStart = gridStart; slotStart.isBefore(gridEnd); slotStart = slotStart.plusMinutes(30)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            if (slotEnd.isAfter(gridEnd)) {
                break;
            }

            boolean busy = busyPredicate.test(slotStart);
            String label = slotStart.toLocalTime().format(SLOT_TIME_LABEL);
            slots.add(new SlotStatusDTO(label, busy ? "busy" : "free"));
        }
        return slots;
    }

    private boolean isSpaceClosedForDeskSearch(
            Long spaceId, LocalDate targetDate, boolean useWindow, LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (spaceId == null) {
            return false;
        }
        if (!useWindow) {
            return spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(spaceId, targetDate);
        }
        LocalDate d1 = windowStart.toLocalDate();
        if (spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(spaceId, d1)) {
            return true;
        }
        LocalDate d2 = windowEnd.toLocalDate();
        return !d2.equals(d1) && spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(spaceId, d2);
    }
}
