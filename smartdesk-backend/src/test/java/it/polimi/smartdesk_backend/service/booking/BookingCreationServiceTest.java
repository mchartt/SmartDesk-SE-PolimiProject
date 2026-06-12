package it.polimi.smartdesk_backend.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.booking.BookingRequestDTO;
import it.polimi.smartdesk_backend.dto.booking.RescheduleBookingDTO;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ConflictException;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.util.policy.BookingTimeRules;

@ExtendWith(MockitoExtension.class)
class BookingCreationServiceTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private DeskRepository deskRepo;
    @Mock
    private BookingDtoMapper bookingDtoMapper;
    @Mock
    private BookingTimeRules bookingTimeRules;
    @Mock
    private DeskStateMachine deskStateMachine;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SpaceManagementService spaceManagementService;

    @InjectMocks
    private BookingCreationService bookingCreationService;

    @Test
    void shouldCreateBookingWhenRequestIsValid() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        BookingRequestDTO request = new BookingRequestDTO(12L, start, end, null);

        Space space = new Space();
        space.setSpaceID(7L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        Booking saved = new Booking();
        saved.setBookingID(99L);
        saved.setDeskID(12L);
        saved.setWorkerID(4L);
        saved.setStatus(BookingStatus.CONFIRMED.name());

        BookingDTO dto = new BookingDTO();
        dto.setBookingID(99L);
        dto.setDeskID(12L);

        when(bookingTimeRules.isBookingDayAllowed(eq(start.toLocalDate()), any(LocalDate.class))).thenReturn(true);
        when(bookingTimeRules.firstBookingSlotStillOpen(eq(start), any(LocalDateTime.class))).thenReturn(true);
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countWorkerOverlapping(4L, start, end, null)).thenReturn(0L);
        when(bookingRepo.countDeskOverlapping(12L, start, end, null)).thenReturn(0L);
        when(bookingRepo.existsByBookingCode(any())).thenReturn(false);
        when(bookingRepo.save(any(Booking.class))).thenReturn(saved);
        when(bookingDtoMapper.toDto(eq(saved), eq(desk), eq(null))).thenReturn(dto);

        BookingDTO result = bookingCreationService.createBooking(4L, request);

        assertNotNull(result);
        assertEquals(99L, result.getBookingID());
        verify(spaceManagementService).assertSpaceOpenOnCalendarDay(space, start.toLocalDate());
    }

    @Test
    void shouldRejectBookingWhenWorkerHasOverlappingSlot() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        BookingRequestDTO request = new BookingRequestDTO(12L, start, end, null);

        Space space = new Space();
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        when(bookingTimeRules.isBookingDayAllowed(eq(start.toLocalDate()), any(LocalDate.class))).thenReturn(true);
        when(bookingTimeRules.firstBookingSlotStillOpen(eq(start), any(LocalDateTime.class))).thenReturn(true);
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countWorkerOverlapping(4L, start, end, null)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> bookingCreationService.createBooking(4L, request));
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenDeskIsAlreadyBooked() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        BookingRequestDTO request = new BookingRequestDTO(12L, start, end, null);

        Space space = new Space();
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        when(bookingTimeRules.isBookingDayAllowed(eq(start.toLocalDate()), any(LocalDate.class))).thenReturn(true);
        when(bookingTimeRules.firstBookingSlotStillOpen(eq(start), any(LocalDateTime.class))).thenReturn(true);
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countWorkerOverlapping(4L, start, end, null)).thenReturn(0L);
        when(bookingRepo.countDeskOverlapping(12L, start, end, null)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> bookingCreationService.createBooking(4L, request));
    }

    @Test
    void shouldRejectRescheduleWhenVersionIsStale() {
        Booking booking = new Booking();
        booking.setBookingID(55L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setVersion(3L);

        RescheduleBookingDTO dto = new RescheduleBookingDTO();
        dto.setVersion(1L);
        dto.setNewStart(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0));
        dto.setNewEnd(dto.getNewStart().plusHours(1));

        when(bookingRepo.findById(55L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> bookingCreationService.rescheduleBooking(4L, 55L, dto));
    }

    @Test
    void shouldPublishReleaseEventWhenRescheduleChangesSlot() {
        LocalDateTime oldStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime oldEnd = oldStart.plusHours(1);
        LocalDateTime newStart = oldStart.plusHours(2);
        LocalDateTime newEnd = oldEnd.plusHours(2);

        Booking booking = new Booking();
        booking.setBookingID(55L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setVersion(3L);
        booking.setBookedDay(oldStart.toLocalDate());
        booking.setStartTime(oldStart);
        booking.setEndTime(oldEnd);

        Space space = new Space();
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        RescheduleBookingDTO dto = new RescheduleBookingDTO();
        dto.setVersion(3L);
        dto.setNewStart(newStart);
        dto.setNewEnd(newEnd);

        BookingDTO response = new BookingDTO();
        response.setBookingID(55L);

        when(bookingRepo.findById(55L)).thenReturn(Optional.of(booking));
        when(bookingTimeRules.isBookingDayAllowed(eq(newStart.toLocalDate()), any(LocalDate.class))).thenReturn(true);
        when(bookingTimeRules.firstBookingSlotStillOpen(eq(newStart), any(LocalDateTime.class))).thenReturn(true);
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countWorkerOverlapping(4L, newStart, newEnd, 55L)).thenReturn(0L);
        when(bookingRepo.countDeskOverlapping(12L, newStart, newEnd, 55L)).thenReturn(0L);
        when(bookingRepo.save(booking)).thenReturn(booking);
        when(bookingDtoMapper.toDto(eq(booking), eq(desk), eq(null))).thenReturn(response);

        BookingDTO result = bookingCreationService.rescheduleBooking(4L, 55L, dto);

        assertEquals(55L, result.getBookingID());
        verify(eventPublisher).publishEvent(any(BookingReleasedEvent.class));
    }

    @Test
    void shouldNotPublishReleaseEventWhenDeskIsInMaintenance() {
        LocalDateTime oldStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime oldEnd = oldStart.plusHours(1);
        LocalDateTime newStart = oldStart.plusHours(2);
        LocalDateTime newEnd = oldEnd.plusHours(2);

        Booking booking = new Booking();
        booking.setBookingID(55L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setVersion(3L);
        booking.setBookedDay(oldStart.toLocalDate());
        booking.setStartTime(oldStart);
        booking.setEndTime(oldEnd);

        Space space = new Space();
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);
        desk.setStateCode(DeskStateCode.MAINTENANCE);

        RescheduleBookingDTO dto = new RescheduleBookingDTO();
        dto.setVersion(3L);
        dto.setNewStart(newStart);
        dto.setNewEnd(newEnd);

        BookingDTO response = new BookingDTO();
        response.setBookingID(55L);

        when(bookingRepo.findById(55L)).thenReturn(Optional.of(booking));
        when(bookingTimeRules.isBookingDayAllowed(eq(newStart.toLocalDate()), any(LocalDate.class))).thenReturn(true);
        when(bookingTimeRules.firstBookingSlotStillOpen(eq(newStart), any(LocalDateTime.class))).thenReturn(true);
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countWorkerOverlapping(4L, newStart, newEnd, 55L)).thenReturn(0L);
        when(bookingRepo.countDeskOverlapping(12L, newStart, newEnd, 55L)).thenReturn(0L);
        when(bookingRepo.save(booking)).thenReturn(booking);
        when(bookingDtoMapper.toDto(eq(booking), eq(desk), eq(null))).thenReturn(response);

        bookingCreationService.rescheduleBooking(4L, 55L, dto);

        verify(eventPublisher, never()).publishEvent(any(BookingReleasedEvent.class));
    }
}
