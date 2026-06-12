package it.polimi.smartdesk_backend.service.space;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.mapper.SpaceMapper;
import it.polimi.smartdesk_backend.dto.space.SpaceRequestDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.admin.SysAdminNotificationService;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;

/** Spazi coworking: approvazione, orari (regola fissa 8-20), desk collegati e regole di accesso host. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class SpaceManagementServiceTest {

    @Mock
    private SpaceRepository spaceRepo;

    @Mock
    private HostRepository hostRepo;

    @Mock
    private SysAdminNotificationService sysAdminNotificationService;

    @Mock
    private ReviewRepository reviewRepo;

    @Mock
    private ReviewStatsService reviewStatsService;

    @Mock
    private HostOwnershipService hostOwnershipService;

    @Spy
    private OpeningHoursService openingHoursService = new OpeningHoursService(new ObjectMapper());

    @Mock
    private SpaceMapper spaceMapper;

    @Mock
    private SpaceClosureRepository spaceClosureRepository;

    @InjectMocks
    private SpaceManagementService SpaceManagementService;

    @BeforeEach
    void setUp() {
        lenient().when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        lenient().when(spaceMapper.toDto(any(Space.class))).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));
    }

    @Test
    void shouldFindSpaceByIdWhenApproved() {
        Space space = new Space();
        space.setSpaceID(11L);
        space.setApproved(true);
        space.setName("Hub");

        when(spaceRepo.findById(11L)).thenReturn(Optional.of(space));
        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of(11L, 4.2));

        SpaceDTO result = SpaceManagementService.findById(11L);

        assertEquals(11L, result.getSpaceID());
        assertEquals(4.2, result.getAverageReviewRating());
    }

    @Test
    void shouldRejectFindByIdWhenNotApproved() {
        Space space = new Space();
        space.setSpaceID(11L);
        space.setApproved(false);

        when(spaceRepo.findById(11L)).thenReturn(Optional.of(space));

        assertThrows(NotFoundException.class, () -> SpaceManagementService.findById(11L));
    }

    @Test
    void shouldListSpacesByHost() {
        Space space = new Space();
        space.setSpaceID(11L);
        space.setHostID(9L);
        space.setApproved(true);

        when(spaceRepo.findByHostID(9L)).thenReturn(List.of(space));

        assertEquals(1, SpaceManagementService.findByHost(9L).size());
    }

    @Test
    void shouldRejectBookingOnClosedCalendarDay() {
        Space space = new Space();
        space.setSpaceID(11L);
        LocalDate day = LocalDate.of(2026, 6, 1);

        when(spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(11L, day)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> SpaceManagementService.assertSpaceOpenOnCalendarDay(space, day));
    }

    @Test
    void shouldRejectCreateSpaceWhenHostNotApproved() {
        Host host = new Host();
        host.setId(9L);
        host.setApproved(false);
        SpaceRequestDTO request = new SpaceRequestDTO();
        request.setName("New");
        request.setDescription("Desc");
        request.setAddress("Addr");
        request.setCity("Milano");

        when(hostOwnershipService.loadHostOrNotFound(9L)).thenReturn(host);

        assertThrows(BusinessRuleException.class, () -> SpaceManagementService.createSpace(9L, request));
    }

    @Test
    void findAllOnlyApproved() {
        Space approved = new Space();
        approved.setSpaceID(1L);
        approved.setName("Approved");
        approved.setCity("Milan");
        approved.setApproved(true);
        approved.addDesk(new Desk());
        approved.addDesk(new Desk());

        Space pending = new Space();
        pending.setSpaceID(2L);
        pending.setName("Pending");
        pending.setApproved(false);

        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(approved));

        List<SpaceDTO> result = SpaceManagementService.findAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSpaceID());
        assertEquals("Milan", result.get(0).getCity());
        assertEquals(2, result.get(0).getDeskCount());
    }

    @Test
    void crudSpaceForHost() {
        Host host = new Host();
        host.setId(9L);
        host.setApproved(true);

        Space space = new Space();
        space.setSpaceID(11L);
        space.setHostID(9L);
        space.setName("Name");
        space.setDescription("Desc");
        space.setAddress("Address");
        space.setCity("Milan");

        SpaceRequestDTO request = new SpaceRequestDTO();
        request.setName("New Name");
        request.setDescription("New Desc");
        request.setAddress("New Address");
        request.setCity("Rome");

        when(hostOwnershipService.loadHostOrNotFound(9L)).thenReturn(host);
        when(spaceRepo.save(any(Space.class))).thenAnswer(inv -> {
            Space s = inv.getArgument(0);
            if (s.getSpaceID() == null) {
                s.setSpaceID(11L);
            }
            return s;
        });
        when(spaceRepo.findById(11L)).thenReturn(Optional.of(space));
        when(hostOwnershipService.loadOwnedSpaceOrNotFound(9L, 11L)).thenReturn(space);

        SpaceDTO created = SpaceManagementService.createSpace(9L, request);
        assertEquals(11L, created.getSpaceID());
        assertFalse(created.isApproved());
        verify(sysAdminNotificationService).notifyAdminsOfPendingSpace(any(Space.class), eq(host));

        SpaceDTO updated = SpaceManagementService.updateSpaceForHost(9L, 11L, request);
        assertEquals("New Name", updated.getName());
        assertEquals("Rome", updated.getCity());

        SpaceManagementService.deleteSpaceForHost(9L, 11L);
        verify(spaceRepo).delete(space);
    }

    @Test
    void hours_okWithinWindow() {
        Space space = new Space();
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDateTime start = monday.atTime(10, 0);
        LocalDateTime end = monday.atTime(11, 0);
        assertDoesNotThrow(() -> SpaceManagementService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void hours_tooEarly() {
        Space space = new Space();
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDateTime start = monday.atTime(7, 0);
        LocalDateTime end = monday.atTime(8, 0);
        assertThrows(BusinessRuleException.class,
                () -> SpaceManagementService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void hours_tooLate() {
        Space space = new Space();
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDateTime start = monday.atTime(20, 30);
        LocalDateTime end = monday.atTime(21, 30);
        assertThrows(BusinessRuleException.class,
                () -> SpaceManagementService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void hours_closedDay() {
        Space space = new Space();
        space.setOpeningHoursJson("{\"MONDAY\":{\"closed\":true}}");
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDateTime start = monday.atTime(10, 0);
        LocalDateTime end = monday.atTime(11, 0);
        assertThrows(BusinessRuleException.class,
                () -> SpaceManagementService.assertBookingWithinOpeningHours(space, start, end));
    }

    @Test
    void hours_exactClosureEnd() {
        Space space = new Space();
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDateTime start = monday.atTime(10, 0);
        LocalDateTime end = monday.atTime(20, 0);
        assertDoesNotThrow(() -> SpaceManagementService.assertBookingWithinOpeningHours(space, start, end));
    }

    private static SpaceDTO dtoFrom(Space space) {
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(space.getSpaceID());
        dto.setName(space.getName());
        dto.setCity(space.getCity());
        dto.setApproved(space.isApproved());
        dto.setOfficeCode(space.getOfficeCode());
        dto.setDeskCount(space.getDesks().size());
        return dto;
    }
}
