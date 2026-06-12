package it.polimi.smartdesk_backend.service.space;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.space.OpeningHoursDayDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Space;

class OpeningHoursServiceTest {

    private OpeningHoursService openingHoursService;

    @BeforeEach
    void setUp() {
        openingHoursService = new OpeningHoursService(new ObjectMapper());
    }

    @Test
    void shouldApplyOpeningHoursFromRequest() throws Exception {
        Space space = new Space();
        OpeningHoursDayDTO monday = new OpeningHoursDayDTO();
        monday.setOpen("09:00");
        monday.setClose("18:00");
        monday.setClosed(false);

        openingHoursService.applyFromRequest(space, Map.of("MONDAY", monday));

        assertTrue(space.getOpeningHoursJson().contains("MONDAY"));
    }

    @Test
    void shouldClearOpeningHoursWhenRequestIsEmpty() {
        Space space = new Space();
        space.setOpeningHoursJson("{\"MONDAY\":{}}");

        openingHoursService.applyFromRequest(space, Map.of());

        assertNull(space.getOpeningHoursJson());
    }

    @Test
    void shouldEnrichDtoFromStoredJson() throws Exception {
        Space space = new Space();
        OpeningHoursDayDTO tuesday = new OpeningHoursDayDTO();
        tuesday.setOpen("10:00");
        tuesday.setClose("19:00");
        tuesday.setClosed(false);
        openingHoursService.applyFromRequest(space, Map.of("TUESDAY", tuesday));

        SpaceDTO dto = new SpaceDTO();
        openingHoursService.enrichDto(space, dto);

        assertEquals("10:00", dto.getOpeningHours().get("TUESDAY").getOpen());
    }

    @Test
    void shouldUseDefaultLimitsWhenJsonMissing() {
        Space space = new Space();
        LocalDate monday = LocalDate.of(2026, 6, 1);

        OpeningHoursDayDTO limits = openingHoursService.getLimitsForDay(space, monday);

        assertEquals("08:00", limits.getOpen());
        assertEquals("20:00", limits.getClose());
        assertEquals(false, limits.isClosed());
    }

    @Test
    void shouldRejectBookingOutsideDefaultOpeningHours() {
        Space space = new Space();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 7, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 9, 0);

        assertThrows(BusinessRuleException.class,
                () -> openingHoursService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void shouldRejectBookingOnClosedDayFromJson() throws Exception {
        Space space = new Space();
        OpeningHoursDayDTO closed = new OpeningHoursDayDTO();
        closed.setClosed(true);
        String dayKey = LocalDate.of(2026, 6, 1).getDayOfWeek().name();
        openingHoursService.applyFromRequest(space, Map.of(dayKey, closed));

        LocalDateTime start = LocalDate.of(2026, 6, 1).atTime(10, 0);
        LocalDateTime end = start.plusHours(2);

        assertThrows(BusinessRuleException.class,
                () -> openingHoursService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void shouldAllowBookingWithinConfiguredHours() throws Exception {
        Space space = new Space();
        OpeningHoursDayDTO openDay = new OpeningHoursDayDTO();
        openDay.setClosed(false);
        openDay.setOpen("09:00");
        openDay.setClose("18:00");
        String dayKey = LocalDate.of(2026, 6, 2).getDayOfWeek().name();
        openingHoursService.applyFromRequest(space, Map.of(dayKey, openDay));

        LocalDateTime start = LocalDate.of(2026, 6, 2).atTime(10, 0);
        LocalDateTime end = start.plusHours(2);

        openingHoursService.assertBookingWithinOpeningHours(space, start, end);
    }
}
