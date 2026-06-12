package it.polimi.smartdesk_backend.service.desk;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.desk.DeskService;

/** CRUD desk, stati e query legate alle prenotazioni per uno spazio. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class DeskServiceTest {

    @Mock
    private DeskRepository deskRepo;

    @Mock
    private SpaceRepository spaceRepo;

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private DeskStateMachine deskStateMachine;

    @InjectMocks
    private DeskService deskService;

    private Desk desk;
    private Space approvedSpace;

    @BeforeEach
    void setUp() {
        approvedSpace = new Space();
        approvedSpace.setSpaceID(10L);
        approvedSpace.setApproved(true);

        desk = new Desk();
        desk.setDeskID(1L);
        desk.setBuilding("Building A");
        desk.setAmenities(List.of("wifi"));
        desk.setStateCode(DeskStateCode.AVAILABLE);
        desk.setSpace(approvedSpace);
        desk.setPricePerHour(5.0);

        approvedSpace.addDesk(desk);
    }

    @Test
    void deskFindById() {
        when(deskRepo.findById(1L)).thenReturn(Optional.of(desk));

        DeskDTO result = deskService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Building A", result.getBuilding());
        assertEquals(5.0, result.getPricePerHour());
    }

    @Test
    void deskFindByIdMissing() {
        when(deskRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deskService.findById(99L));
    }

    @Test
    void publicCatalogShowsOnlyDesksInApprovedSpaces() {
        Space pending = new Space();
        pending.setSpaceID(11L);
        pending.setApproved(false);
        Desk hidden = new Desk();
        hidden.setDeskID(2L);
        hidden.setBuilding("Hidden");
        hidden.setStateCode(DeskStateCode.AVAILABLE);
        hidden.setSpace(pending);

        when(deskRepo.findAllApprovedWithSpaceAndRoom()).thenReturn(List.of(desk));

        List<DeskDTO> result = deskService.findAllApproved();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void catalogDetailHidesUnapprovedDesk() {
        Space pending = new Space();
        pending.setSpaceID(11L);
        pending.setApproved(false);
        Desk hidden = new Desk();
        hidden.setDeskID(2L);
        hidden.setSpace(pending);

        when(deskRepo.findById(2L)).thenReturn(Optional.of(hidden));

        assertThrows(NotFoundException.class, () -> deskService.findApprovedById(2L));
    }

    @Test
    void findAvailableFiltersOutAlreadyBooked() {
        LocalDate today = LocalDate.now().plusDays(1);
        when(deskRepo.findAllApprovedWithSpaceAndRoom()).thenReturn(List.of(desk));
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(bookingRepo.findActiveNonCancelledOverlappingDay(1L, today)).thenReturn(List.of());

        List<DeskDTO> result = deskService.findAvailable(today);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void deskFindAll() {
        when(deskRepo.findAllWithSpaceAndRoom()).thenReturn(List.of(desk));

        List<DeskDTO> result = deskService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void deskForApprovedSpace() {
        when(spaceRepo.findById(10L)).thenReturn(Optional.of(approvedSpace));
        when(deskRepo.findBySpaceSpaceIDWithSpaceAndRoom(10L)).thenReturn(List.of(desk));

        List<DeskDTO> result = deskService.findBySpace(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getSpaceID());
    }

    @Test
    void deskForMissingSpace() {
        when(spaceRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deskService.findBySpace(99L));
    }

    @Test
    void deskForPendingSpace() {
        Space unapproved = new Space();
        unapproved.setSpaceID(20L);
        unapproved.setApproved(false);

        when(spaceRepo.findById(20L)).thenReturn(Optional.of(unapproved));

        assertThrows(NotFoundException.class, () -> deskService.findBySpace(20L));
    }

    @Test
    void availableExcludeBooked() {
        LocalDate today = LocalDate.now().plusDays(1);

        Desk bookedDesk = new Desk();
        bookedDesk.setDeskID(2L);
        bookedDesk.setBuilding("Building B");
        bookedDesk.setStateCode(DeskStateCode.AVAILABLE);
        bookedDesk.setSpace(approvedSpace);

        Booking booking = new Booking();
        booking.setDeskID(2L);
        booking.setBookedDay(today);

        when(deskRepo.findAllApprovedWithSpaceAndRoom()).thenReturn(List.of(desk, bookedDesk));
        when(deskStateMachine.isBookable(desk)).thenReturn(true);
        when(deskStateMachine.isBookable(bookedDesk)).thenReturn(true);
        when(bookingRepo.findActiveNonCancelledOverlappingDay(1L, today)).thenReturn(List.of());
        when(bookingRepo.findActiveNonCancelledOverlappingDay(2L, today)).thenReturn(List.of(booking));

        List<DeskDTO> result = deskService.findAvailable(today);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void findAllDeskWithOrWithoutSpace() {
        Desk deskWithoutSpace = new Desk();
        deskWithoutSpace.setDeskID(3L);
        deskWithoutSpace.setBuilding("Orphan");
        deskWithoutSpace.setStateCode(DeskStateCode.AVAILABLE);
        // space == null: non deve esplodere in NPE

        when(deskRepo.findAllWithSpaceAndRoom()).thenReturn(List.of(desk, deskWithoutSpace));

        List<DeskDTO> result = deskService.findAll();

        assertEquals(2, result.size());
        boolean hasOrphan = result.stream().anyMatch(d -> d.getId().equals(3L));
        assertTrue(hasOrphan);
    }
}
