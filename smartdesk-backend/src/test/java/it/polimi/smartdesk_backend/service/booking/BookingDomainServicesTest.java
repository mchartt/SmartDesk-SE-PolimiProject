package it.polimi.smartdesk_backend.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.booking.SearchCriteriaDTO;
import it.polimi.smartdesk_backend.dto.booking.WaitlistStatusDTO;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.OpeningHoursDayDTO;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.booking.WaitlistEntry;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.space.OpeningHoursService;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.booking.WaitlistEntryRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
class BookingDomainServicesTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private DeskRepository deskRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private BookingDtoMapper bookingDtoMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private WaitlistEntryRepository waitlistEntryRepo;

    @Mock
    private TicketRepository ticketRepo;
    @Mock
    private TicketWorkerNoteRepository workerNoteRepo;
    @Mock
    private TicketTechnicianNoteRepository technicianNoteRepo;
    @Mock
    private TicketHostNoteRepository hostNoteRepo;
    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private SpaceClosureRepository spaceClosureRepository;
    @Mock
    private DeskStateMachine deskStateMachine;
    @Mock
    private ReviewStatsService reviewStatsService;
    @Mock
    private OpeningHoursService openingHoursService;

    private BookingQueryService bookingQueryService;
    private BookingCancellationService bookingCancellationService;
    private BookingWaitlistService bookingWaitlistService;
    private BookingWorkerHistoryService bookingWorkerHistoryService;
    private BookingEndSessionService bookingEndSessionService;
    private DeskAvailabilityService deskAvailabilityService;

    @BeforeEach
    void setUp() {
        bookingQueryService = new BookingQueryService(
                bookingRepo, reviewRepo, deskRepo, userRepo, bookingDtoMapper);
        bookingCancellationService = new BookingCancellationService(
                bookingRepo, deskRepo, notificationService, eventPublisher);
        bookingWaitlistService = new BookingWaitlistService(
                deskRepo, bookingRepo, waitlistEntryRepo, notificationService);
        bookingWorkerHistoryService = new BookingWorkerHistoryService(
                bookingRepo, reviewRepo, ticketRepo, workerNoteRepo, technicianNoteRepo, hostNoteRepo);
        bookingEndSessionService = new BookingEndSessionService(
                bookingRepo, deskRepo, bookingDtoMapper, eventPublisher);
        deskAvailabilityService = new DeskAvailabilityService(
                bookingRepo, deskRepo, spaceRepo, spaceClosureRepository,
                deskStateMachine, reviewStatsService, openingHoursService);
    }

  // --- BookingQueryService ---

    @Test
    void shouldListBookingsByWorker() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());

        when(bookingRepo.findByWorkerID(4L)).thenReturn(List.of(booking));
        when(deskRepo.findAllWithSpaceAndRoomByDeskIdIn(any())).thenReturn(List.of());
        when(bookingDtoMapper.toDtoList(any(), any(), any())).thenReturn(List.of(new BookingDTO()));

        assertEquals(1, bookingQueryService.getBookingsByWorker(4L).size());
    }

    @Test
    void shouldFindBookingByIdForAdmin() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(99L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        BookingDTO dto = new BookingDTO();

        booking.setDeskID(12L);
        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.findAllWithSpaceAndRoomByDeskIdIn(any())).thenReturn(List.of(desk));
        when(bookingDtoMapper.toDto(booking, desk, null)).thenReturn(dto);

        BookingDTO result = bookingQueryService.findByIdForUser(7L, 1L, Role.SYS_ADMIN);

        assertEquals(dto, result);
    }

    @Test
    void shouldFindBookingById() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setDeskID(12L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        BookingDTO dto = new BookingDTO();
        dto.setBookingID(7L);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.findAllWithSpaceAndRoomByDeskIdIn(any())).thenReturn(List.of(desk));
        when(bookingDtoMapper.toDto(booking, desk, null)).thenReturn(dto);

        BookingDTO result = bookingQueryService.findById(7L);

        assertEquals(7L, result.getBookingID());
    }

    @Test
    void shouldHideBookingFromOtherWorker() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(99L);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class,
                () -> bookingQueryService.findByIdForUser(7L, 4L, Role.WORKER));
    }

    @Test
    void shouldReturnReviewEligibleBookings() {
        LocalDateTime now = LocalDateTime.now();
        Booking eligible = new Booking();
        eligible.setBookingID(1L);
        eligible.setWorkerID(4L);
        eligible.setDeskID(12L);
        eligible.setStatus(BookingStatus.CONFIRMED.name());
        eligible.setEndTime(now.minusDays(1));

        when(bookingRepo.findByWorkerID(4L)).thenReturn(List.of(eligible));
        when(reviewRepo.existsByBookingID(1L)).thenReturn(false);
        when(deskRepo.findAllWithSpaceAndRoomByDeskIdIn(any())).thenReturn(List.of());
        when(bookingDtoMapper.toDtoList(any(), any(), eq(Map.of()))).thenReturn(List.of(new BookingDTO()));

        List<BookingDTO> result = bookingQueryService.getReviewEligibleBookings(4L);

        assertEquals(1, result.size());
    }

  // --- BookingCancellationService ---

    @Test
    void shouldCancelBookingAndPublishReleaseEvent() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setStartTime(LocalDateTime.now().plusHours(2));
        booking.setEndTime(LocalDateTime.now().plusHours(4));
        booking.setBookedDay(LocalDate.now().plusDays(1));

        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.save(booking)).thenReturn(booking);

        bookingCancellationService.removeBooking(7L);

        assertEquals(BookingStatus.CANCELLED.name(), booking.getStatus());
        verify(eventPublisher).publishEvent(any(BookingReleasedEvent.class));
    }

    @Test
    void shouldSkipCancellationWhenAlreadyCancelled() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CANCELLED.name());

        Desk desk = new Desk();
        desk.setDeskID(12L);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));

        bookingCancellationService.removeBooking(7L);

        verify(bookingRepo, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldCancelBookingAsAdminAndNotifyWorker() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setBookingCode("AB12CD");
        booking.setStartTime(LocalDateTime.now().plusHours(2));
        booking.setEndTime(LocalDateTime.now().plusHours(4));
        booking.setBookedDay(LocalDate.now().plusDays(1));

        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.save(booking)).thenReturn(booking);

        bookingCancellationService.removeBookingForUser(7L, 1L, Role.SYS_ADMIN);

        assertEquals(BookingStatus.CANCELLED.name(), booking.getStatus());
        verify(notificationService).notifyBookingCancelledByAdmin(4L, "AB12CD");
    }

    @Test
    void shouldRejectWorkerCancellationWhenBookingAlreadyStarted() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setStartTime(LocalDateTime.now().minusMinutes(5));

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessRuleException.class,
                () -> bookingCancellationService.removeBookingForUser(7L, 4L, Role.WORKER));
    }

  // --- BookingWaitlistService ---

    @Test
    void shouldSubscribeWorkerToWaitlist() {
        LocalDate day = LocalDate.now().plusDays(2);
        LocalDateTime start = day.atTime(10, 0);
        LocalDateTime end = day.atTime(12, 0);
        Desk desk = new Desk();
        desk.setDeskID(12L);

        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(waitlistEntryRepo.findByWorkerIDAndDeskIDAndBookedDay(4L, 12L, day)).thenReturn(Optional.empty());
        when(waitlistEntryRepo.save(any(WaitlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingWaitlistService.notifyMeWhenAvailable(12L, day, 4L, start, end);

        verify(notificationService).notifyWaitlistSubscription(4L, 12L, day);
        verify(waitlistEntryRepo).save(any(WaitlistEntry.class));
    }

    @Test
    void shouldReturnWaitlistStatusWhenNotSubscribed() {
        LocalDate day = LocalDate.now().plusDays(2);
        Desk desk = new Desk();
        desk.setDeskID(12L);

        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(waitlistEntryRepo.findByWorkerIDAndDeskIDAndBookedDay(4L, 12L, day)).thenReturn(Optional.empty());

        WaitlistStatusDTO status = bookingWaitlistService.getWaitlistStatus(12L, day, 4L);

        assertFalse(status.isSubscribed());
        assertFalse(status.isNotified());
    }

    @Test
    void shouldNotifyFirstMatchingWaitlistEntryOnReleasedSlot() {
        LocalDate day = LocalDate.now().plusDays(2);
        LocalDateTime start = day.atTime(10, 0);
        LocalDateTime end = day.atTime(12, 0);

        WaitlistEntry entry = new WaitlistEntry();
        entry.setWorkerID(4L);
        entry.setDeskID(12L);
        entry.setBookedDay(day);
        entry.setDesiredStartTime(start);
        entry.setDesiredEndTime(end);
        entry.setNotified(false);

        Desk desk = new Desk();
        desk.setDeskID(12L);

        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.countDeskOverlapping(12L, start, end, null)).thenReturn(0L);
        when(waitlistEntryRepo.findByDeskIDAndBookedDayAndNotifiedFalseOrderByCreatedAtAsc(12L, day))
                .thenReturn(List.of(entry));
        when(waitlistEntryRepo.save(entry)).thenReturn(entry);

        bookingWaitlistService.handleReleasedSlot(12L, day, start, end);

        assertTrue(entry.isNotified());
        verify(notificationService).notifyDeskAvailability(4L, 12L, day);
    }

  // --- BookingWorkerHistoryService ---

    @Test
    void shouldClearPastBookingsForWorker() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = new Booking();
        past.setBookingID(1L);
        past.setWorkerID(4L);
        past.setDeskID(12L);
        past.setStartTime(now.minusDays(2).withHour(10));
        past.setEndTime(now.minusDays(2).withHour(12));

        when(bookingRepo.findByWorkerID(4L)).thenReturn(List.of(past));
        when(reviewRepo.findByBookingIDIn(List.of(1L))).thenReturn(List.of());
        when(ticketRepo.findByDeskID(12L)).thenReturn(List.of());

        int removed = bookingWorkerHistoryService.clearPastBookingsForWorker(4L);

        assertEquals(1, removed);
        verify(bookingRepo).deleteAllById(List.of(1L));
    }

    @Test
    void shouldReturnZeroWhenNoPastBookingsToClear() {
        Booking future = new Booking();
        future.setBookingID(2L);
        future.setWorkerID(4L);
        future.setStartTime(LocalDateTime.now().plusDays(1));

        when(bookingRepo.findByWorkerID(4L)).thenReturn(List.of(future));

        assertEquals(0, bookingWorkerHistoryService.clearPastBookingsForWorker(4L));
        verify(bookingRepo, never()).deleteAllById(any());
    }

  // --- BookingEndSessionService ---

    @Test
    void shouldEndActiveSessionForWorker() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(4L);
        booking.setDeskID(12L);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setStartTime(now.minusMinutes(30));
        booking.setEndTime(now.plusHours(2));
        booking.setBookedDay(now.toLocalDate());

        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setStateCode(DeskStateCode.AVAILABLE);
        BookingDTO dto = new BookingDTO();
        dto.setBookingID(7L);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));
        when(deskRepo.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(bookingRepo.save(booking)).thenReturn(booking);
        when(bookingDtoMapper.toDto(eq(booking), eq(desk), isNull())).thenReturn(dto);

        BookingDTO result = bookingEndSessionService.endSessionForWorker(4L, 7L);

        assertEquals(7L, result.getBookingID());
        verify(eventPublisher).publishEvent(any(BookingReleasedEvent.class));
    }

    @Test
    void shouldRejectEndSessionForOtherWorker() {
        Booking booking = new Booking();
        booking.setBookingID(7L);
        booking.setWorkerID(99L);

        when(bookingRepo.findById(7L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingEndSessionService.endSessionForWorker(4L, 7L));
    }

  // --- DeskAvailabilityService ---

    @Test
    void shouldMarkAllSlotsBusyWhenSpaceClosed() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        OpeningHoursDayDTO limits = new OpeningHoursDayDTO();
        limits.setOpen("09:00");
        limits.setClose("21:00");
        limits.setClosed(false);

        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(openingHoursService.getLimitsForDay(space, day)).thenReturn(limits);
        when(spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(7L, day)).thenReturn(true);

        var slots = deskAvailabilityService.getSlotAvailability(12L, day);

        assertEquals(24, slots.size());
        assertTrue(slots.stream().allMatch(slot -> "busy".equals(slot.getStatus())));
    }

    @Test
    void shouldMarkOverlappingSlotsBusy() {
        LocalDate day = LocalDate.of(2026, 5, 21);
        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        OpeningHoursDayDTO limits = new OpeningHoursDayDTO();
        limits.setOpen("09:00");
        limits.setClose("12:00");
        limits.setClosed(false);

        Booking booking = new Booking();
        booking.setStartTime(day.atTime(9, 30));
        booking.setEndTime(day.atTime(10, 30));
        booking.setStatus(BookingStatus.CONFIRMED.name());

        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(openingHoursService.getLimitsForDay(space, day)).thenReturn(limits);
        when(spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(7L, day)).thenReturn(false);
        when(bookingRepo.findActiveNonCancelledOverlappingDay(12L, day)).thenReturn(List.of(booking));

        var slots = deskAvailabilityService.getSlotAvailability(12L, day);

        assertTrue(slots.stream().anyMatch(s -> "09:30".equals(s.getTime()) && "busy".equals(s.getStatus())));
        assertTrue(slots.stream().anyMatch(s -> "09:00".equals(s.getTime()) && "free".equals(s.getStatus())));
    }

    @Test
    void shouldSearchBookableDesksInApprovedSpace() {
        LocalDate day = LocalDate.now().plusDays(3);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);
        criteria.setRequiredAmenities(List.of("WIFI"));

        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of("WIFI", "MONITOR"));
        space.addDesk(desk);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(bookingRepo.findByDeskIDAndBookedDay(12L, day)).thenReturn(List.of());

        List<DeskDTO> result = deskAvailabilityService.searchDesks(criteria);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getBookable());
    }

    @Test
    void shouldFilterSearchByCityAndSpaceId() {
        LocalDate day = LocalDate.now().plusDays(3);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);
        criteria.setCity("Milano");
        criteria.setSpaceId(7L);

        Space milano = EntityTestFixtures.spaceMilano(7L, 4L);
        milano.setCity("Milano");
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of());
        milano.addDesk(desk);

        Space rome = EntityTestFixtures.spaceMilano(8L, 4L);
        rome.setCity("Roma");
        Desk other = new Desk();
        other.setDeskID(13L);
        other.setAmenities(List.of());
        rome.addDesk(other);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(milano, rome));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(bookingRepo.findByDeskIDAndBookedDay(12L, day)).thenReturn(List.of());

        List<DeskDTO> result = deskAvailabilityService.searchDesks(criteria);

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).getId());
    }

    @Test
    void shouldSearchDesksUsingTimeWindowOverlap() {
        LocalDate day = LocalDate.now().plusDays(2);
        LocalDateTime start = day.atTime(10, 0);
        LocalDateTime end = day.atTime(12, 0);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);
        criteria.setStartTime(start);
        criteria.setEndTime(end);

        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of());
        space.addDesk(desk);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(bookingRepo.countDeskOverlapping(12L, start, end, null)).thenReturn(0L);

        assertEquals(1, deskAvailabilityService.searchDesks(criteria).size());
    }

    @Test
    void shouldIncludeMaintenanceDeskWhenRequested() {
        LocalDate day = LocalDate.now().plusDays(3);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);
        criteria.setIncludeMaintenance(true);
        criteria.setRequiredAmenities(List.of("WIFI"));

        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of("WIFI"));
        desk.setStateCode(DeskStateCode.MAINTENANCE);
        space.addDesk(desk);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(deskStateMachine.isBookable(desk)).thenReturn(false);

        List<DeskDTO> result = deskAvailabilityService.searchDesks(criteria);

        assertEquals(1, result.size());
        assertEquals(DeskStateCode.MAINTENANCE.name(), result.get(0).getCurrentState());
        assertEquals(false, result.get(0).getBookable());
    }

    @Test
    void shouldSkipClosedSpaceOnDeskSearch() {
        LocalDate day = LocalDate.now().plusDays(3);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);

        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of());
        space.addDesk(desk);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(7L, day)).thenReturn(true);

        assertTrue(deskAvailabilityService.searchDesks(criteria).isEmpty());
    }

    @Test
    void shouldMarkAllSlotsBusyWhenOpeningHoursDayIsClosed() {
        LocalDate day = LocalDate.of(2026, 5, 22);
        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        OpeningHoursDayDTO limits = new OpeningHoursDayDTO();
        limits.setClosed(true);

        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(openingHoursService.getLimitsForDay(space, day)).thenReturn(limits);

        var slots = deskAvailabilityService.getSlotAvailability(12L, day);

        assertTrue(slots.stream().allMatch(slot -> "busy".equals(slot.getStatus())));
    }

    @Test
    void shouldExcludeDeskWithActiveBookingOnSearchDay() {
        LocalDate day = LocalDate.now().plusDays(3);
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setTargetDate(day);

        Space space = EntityTestFixtures.spaceMilano(7L, 4L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setAmenities(List.of());
        space.addDesk(desk);

        Booking active = new Booking();
        active.setStatus(BookingStatus.CONFIRMED.name());

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(bookingRepo.findByDeskIDAndBookedDay(12L, day)).thenReturn(List.of(active));

        List<DeskDTO> result = deskAvailabilityService.searchDesks(criteria);

        assertTrue(result.isEmpty());
    }
}
