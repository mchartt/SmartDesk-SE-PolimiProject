package it.polimi.smartdesk_backend.dto;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.booking.RescheduleBookingDTO;
import it.polimi.smartdesk_backend.dto.booking.SearchCriteriaDTO;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.model.booking.Booking;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Controlla se le validazioni di spring bloccano le cose strane (liste vuote, constraint saltati). */
@FieldDefaults(level = AccessLevel.PRIVATE)
class DtoEdgeCasesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bookingDtoCompactConstructor() {
        LocalDateTime end = LocalDateTime.of(2026, 5, 1, 18, 0);
        BookingDTO dto = new BookingDTO(9L, 3L, end.minusHours(2), end);
        assertEquals(9L, dto.getBookingID());
        assertEquals(3L, dto.getDeskID());
        assertEquals(end, dto.getEndTime());
    }

    @Test
    void bookingDtoExposesOptimisticVersion() {
        Booking booking = new Booking();
        booking.setBookingID(9L);
        booking.setDeskID(3L);
        booking.setStartTime(LocalDateTime.of(2026, 5, 1, 9, 0));
        booking.setEndTime(LocalDateTime.of(2026, 5, 1, 18, 0));
        booking.setVersion(4L);

        BookingDTO dto = BookingDtoMapper.fromBookingSkeleton(booking);

        assertEquals(4L, dto.getVersion());
    }

    @Test
    void rescheduleRequiresOptimisticVersion() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        RescheduleBookingDTO dto = new RescheduleBookingDTO(9L, null, start, start.plusHours(2));

        Set<ConstraintViolation<RescheduleBookingDTO>> errors = validator.validate(dto);

        assertFalse(errors.isEmpty());
    }

    @Test
    void ticketRequiresBookingOrDeskCode() {
        TicketDTO vuoto = new TicketDTO();
        vuoto.setTitle("Monitor rotto");
        vuoto.setDescription("Monitor spento");
        Set<ConstraintViolation<TicketDTO>> errors = validator.validate(vuoto);
        assertFalse(errors.isEmpty());

        TicketDTO conBooking = new TicketDTO();
        conBooking.setBookingID(1L);
        conBooking.setTitle("Monitor rotto");
        conBooking.setDescription("Monitor spento");
        assertTrue(validator.validate(conBooking).isEmpty());
    }

    @Test
    void deskDtoDefensivelyCopiesAmenitiesFromConstructorAndSetter() {
        List<String> sourceAmenities = new ArrayList<>(List.of("wifi"));
        DeskDTO dto = new DeskDTO(1L, "A1", "Building A", sourceAmenities);
        sourceAmenities.add("monitor");

        assertEquals(List.of("wifi"), dto.getAmenities());

        List<String> newAmenities = new ArrayList<>(List.of("quiet"));
        dto.setAmenities(newAmenities);
        newAmenities.add("window");

        assertEquals(List.of("quiet"), dto.getAmenities());
        assertNotSame(newAmenities, dto.getAmenities());
    }

    @Test
    void deskDtoHandlesNullAmenitiesAsEmptyList() {
        DeskDTO dtoFromConstructor = new DeskDTO(1L, "A1", "Building A", null);
        DeskDTO dtoFromSetter = new DeskDTO();
        dtoFromSetter.setAmenities(null);

        assertTrue(dtoFromConstructor.getAmenities().isEmpty());
        assertTrue(dtoFromSetter.getAmenities().isEmpty());
    }

    @Test
    void searchCriteriaDefensivelyCopiesRequiredAmenitiesAndHandlesNull() {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        List<String> required = new ArrayList<>(List.of("wifi"));

        criteria.setRequiredAmenities(required);
        required.add("monitor");

        assertEquals(List.of("wifi"), criteria.getRequiredAmenities());
        assertNotSame(required, criteria.getRequiredAmenities());

        criteria.setRequiredAmenities(null);
        assertTrue(criteria.getRequiredAmenities().isEmpty());
    }
}
