package it.polimi.smartdesk_backend.service.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.space.DeskRequestDTO;
import it.polimi.smartdesk_backend.dto.space.RoomDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceAmenityPresetDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.RoomMapper;
import it.polimi.smartdesk_backend.mapper.SpaceAmenityPresetMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Room;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.space.SpaceAmenityPreset;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.RoomRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceAmenityPresetRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class HostDomainServicesTest {

    private static final Long HOST_ID = 4L;
    private static final Long SPACE_ID = 10L;

    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private DeskRepository deskRepo;
    @Mock
    private RoomRepository roomRepo;
    @Mock
    private SpaceAmenityPresetRepository amenityPresetRepo;
    @Mock
    private HostOwnershipService hostOwnershipService;
    @Mock
    private DeskStateMachine deskStateMachine;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private SpaceAmenityPresetMapper spaceAmenityPresetMapper;

    private HostDeskService hostDeskService;
    private HostRoomService hostRoomService;
    private HostAmenityPresetService hostAmenityPresetService;

    @BeforeEach
    void setUp() {
        hostDeskService = new HostDeskService(spaceRepo, deskRepo, roomRepo, hostOwnershipService, deskStateMachine);
        hostRoomService = new HostRoomService(deskRepo, roomRepo, roomMapper, hostOwnershipService);
        hostAmenityPresetService = new HostAmenityPresetService(
                spaceRepo, amenityPresetRepo, spaceAmenityPresetMapper, hostOwnershipService);
    }

  // --- HostDeskService ---

    @Test
    void shouldRejectDeskCreationWithoutSpaceId() {
        DeskRequestDTO request = new DeskRequestDTO();
        request.setRoomID(11L);

        assertThrows(NotFoundException.class, () -> hostDeskService.createDesk(HOST_ID, request));
    }

    @Test
    void shouldRejectDeskCreationWhenRoomBelongsToAnotherSpace() {
        DeskRequestDTO request = new DeskRequestDTO();
        request.setSpaceID(SPACE_ID);
        request.setRoomID(11L);

        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Space otherSpace = EntityTestFixtures.spaceMilano(99L, 2L);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(otherSpace);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));

        assertThrows(NotFoundException.class, () -> hostDeskService.createDesk(HOST_ID, request));
    }

    @Test
    void shouldRejectDeskCreationWhenGeneratedCodeAlreadyExists() {
        DeskRequestDTO request = new DeskRequestDTO();
        request.setSpaceID(SPACE_ID);
        request.setRoomID(11L);

        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(space);
        room.setCode("TA");

        Desk existing = new Desk();
        existing.setDeskID(99L);
        existing.setCode("TA1");

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));
        when(deskRepo.findByRoom_RoomID(11L)).thenReturn(List.of());
        when(deskRepo.findBySpace_SpaceIDAndCode(SPACE_ID, "TA1")).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () -> hostDeskService.createDesk(HOST_ID, request));
    }

    @Test
    void shouldMoveDeskToAnotherRoomAndRegenerateCode() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room oldRoom = new Room();
        oldRoom.setRoomID(11L);
        oldRoom.setSpace(space);
        oldRoom.setCode("TA");
        oldRoom.setName("Open");

        Room newRoom = new Room();
        newRoom.setRoomID(12L);
        newRoom.setSpace(space);
        newRoom.setCode("TB");
        newRoom.setName("Quiet");

        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);
        desk.setRoom(oldRoom);
        desk.setCode("TA1");
        desk.setBuilding("Open");

        DeskRequestDTO request = new DeskRequestDTO();
        request.setRoomID(12L);
        request.setAmenities(List.of("WIFI"));

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));
        when(roomRepo.findById(12L)).thenReturn(Optional.of(newRoom));
        when(deskRepo.findByRoom_RoomID(12L)).thenReturn(List.of());
        when(deskRepo.findBySpace_SpaceIDAndCode(SPACE_ID, "TB1")).thenReturn(Optional.empty());
        when(deskRepo.save(desk)).thenReturn(desk);

        var result = hostDeskService.updateDesk(20L, request);

        assertEquals("TB1", result.getCode());
        assertEquals(12L, desk.getRoom().getRoomID());
        assertEquals("Quiet", desk.getBuilding());
    }

    @Test
    void shouldDeleteDeskDirectlyWhenNotAttachedToSpaceAggregate() {
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(null);

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));

        hostDeskService.removeDesk(20L);

        verify(deskRepo).delete(desk);
        verify(spaceRepo, never()).save(any());
    }

    @Test
    void shouldRejectDeskCreationWithoutRoomId() {
        DeskRequestDTO request = new DeskRequestDTO();
        request.setSpaceID(SPACE_ID);

        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);

        assertThrows(BusinessRuleException.class, () -> hostDeskService.createDesk(HOST_ID, request));
    }

    @Test
    void shouldCreateDeskInOwnedSpace() {
        DeskRequestDTO request = new DeskRequestDTO();
        request.setSpaceID(SPACE_ID);
        request.setRoomID(11L);
        request.setAmenities(List.of("WIFI"));

        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(space);
        room.setCode("TA");
        room.setName("Open Space");

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));
        when(deskRepo.findByRoom_RoomID(11L)).thenReturn(List.of());
        when(deskRepo.findBySpace_SpaceIDAndCode(SPACE_ID, "TA1")).thenReturn(Optional.empty());
        when(deskRepo.save(any(Desk.class))).thenAnswer(invocation -> {
            Desk saved = invocation.getArgument(0);
            saved.setDeskID(20L);
            return saved;
        });

        var result = hostDeskService.createDesk(HOST_ID, request);

        assertEquals(20L, result.getId());
        assertEquals("TA1", result.getCode());
        verify(deskRepo).save(any(Desk.class));
    }

    @Test
    void shouldDecommissionDeskForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));
        when(deskRepo.save(desk)).thenReturn(desk);

        hostDeskService.decommissionDesk(HOST_ID, 20L);

        verify(deskStateMachine).decommission(desk);
        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, desk, 20L);
    }

  // --- HostRoomService ---

    @Test
    void shouldListRoomsForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setName("Open Space");
        RoomDTO dto = new RoomDTO();
        dto.setRoomID(11L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findBySpace_SpaceIDOrderByNameAsc(SPACE_ID)).thenReturn(List.of(room));
        when(roomMapper.toDto(room)).thenReturn(dto);

        List<RoomDTO> result = hostRoomService.listRoomsForHost(HOST_ID, SPACE_ID);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getRoomID());
    }

    @Test
    void shouldCreateRoomForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        RoomDTO body = new RoomDTO();
        body.setName("Meeting");
        body.setCode("MR");

        Room saved = new Room();
        saved.setRoomID(11L);
        saved.setCode("MR");
        RoomDTO dto = new RoomDTO();
        dto.setRoomID(11L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.existsBySpace_SpaceIDAndCode(SPACE_ID, "MR")).thenReturn(false);
        when(roomRepo.save(any(Room.class))).thenReturn(saved);
        when(roomMapper.toDto(saved)).thenReturn(dto);

        RoomDTO result = hostRoomService.createRoomForHost(HOST_ID, SPACE_ID, body);

        assertEquals(11L, result.getRoomID());
    }

    @Test
    void shouldRejectRoomDeletionWhenDesksExist() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(space);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));
        when(deskRepo.countByRoom_RoomID(11L)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> hostRoomService.deleteRoomForHost(HOST_ID, SPACE_ID, 11L));
    }

  // --- HostAmenityPresetService ---

    @Test
    void shouldListAmenityPresetsForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        SpaceAmenityPreset preset = new SpaceAmenityPreset();
        preset.setPresetID(1L);
        SpaceAmenityPresetDTO dto = new SpaceAmenityPresetDTO();
        dto.setPresetID(1L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(SPACE_ID)).thenReturn(List.of(preset));
        when(spaceAmenityPresetMapper.toDto(preset)).thenReturn(dto);

        List<SpaceAmenityPresetDTO> result = hostAmenityPresetService.listAmenityPresetsForHost(HOST_ID, SPACE_ID);

        assertEquals(1, result.size());
    }

    @Test
    void shouldCreateAmenityPresetForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        SpaceAmenityPresetDTO body = new SpaceAmenityPresetDTO();
        body.setLabel("Configurazione monitor");
        body.setHint("Include doppio monitor");
        body.setAmenities(List.of("MONITOR"));

        SpaceAmenityPresetDTO dto = new SpaceAmenityPresetDTO();
        dto.setLabel("Configurazione monitor");

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(SPACE_ID)).thenReturn(List.of());
        when(spaceAmenityPresetMapper.toDto(any(SpaceAmenityPreset.class))).thenReturn(dto);

        SpaceAmenityPresetDTO result = hostAmenityPresetService.createAmenityPresetForHost(HOST_ID, SPACE_ID, body);

        assertEquals("Configurazione monitor", result.getLabel());
        verify(spaceRepo).save(space);
        assertEquals(1, space.getAmenityPresets().size());
    }

    @Test
    void shouldUpdateRoomForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(space);
        room.setCode("MR");
        room.setName("Old");

        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setRoom(room);
        desk.setSpace(space);
        desk.setCode("MR1");

        RoomDTO body = new RoomDTO();
        body.setName("Meeting");
        body.setCode("MR");
        RoomDTO dto = new RoomDTO();
        dto.setRoomID(11L);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));
        when(roomRepo.save(any(Room.class))).thenReturn(room);
        when(deskRepo.findByRoom_RoomID(11L)).thenReturn(List.of(desk));
        when(deskRepo.save(desk)).thenReturn(desk);
        when(roomMapper.toDto(room)).thenReturn(dto);

        RoomDTO result = hostRoomService.updateRoomForHost(HOST_ID, SPACE_ID, 11L, body);

        assertEquals(11L, result.getRoomID());
        assertEquals("Meeting", desk.getBuilding());
    }

    @Test
    void shouldDeleteRoomWhenNoDesksRemain() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Room room = new Room();
        room.setRoomID(11L);
        room.setSpace(space);

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(roomRepo.findById(11L)).thenReturn(Optional.of(room));
        when(deskRepo.countByRoom_RoomID(11L)).thenReturn(0L);

        hostRoomService.deleteRoomForHost(HOST_ID, SPACE_ID, 11L);

        verify(roomRepo).delete(room);
    }

    @Test
    void shouldUpdateDeskAmenitiesForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);
        desk.setCode("TA1");

        DeskRequestDTO request = new DeskRequestDTO();
        request.setAmenities(List.of("WIFI", "MONITOR"));

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));
        when(deskRepo.save(desk)).thenReturn(desk);

        var result = hostDeskService.updateDeskForHost(HOST_ID, 20L, request);

        assertEquals(20L, result.getId());
        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, desk, 20L);
    }

    @Test
    void shouldRemoveDeskFromSpaceForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);
        space.addDesk(desk);

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));

        hostDeskService.removeDeskForHost(HOST_ID, 20L);

        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, desk, 20L);
        verify(spaceRepo).save(space);
    }

    @Test
    void shouldApproveDeskInspectionForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));
        when(deskRepo.save(desk)).thenReturn(desk);

        var result = hostDeskService.approveInspection(HOST_ID, 20L);

        assertEquals(20L, result.getId());
        verify(deskStateMachine).completeInspection(desk);
    }

    @Test
    void shouldRejectDeskInspectionForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(20L);
        desk.setSpace(space);

        when(deskRepo.findById(20L)).thenReturn(Optional.of(desk));
        when(deskRepo.save(desk)).thenReturn(desk);

        hostDeskService.rejectInspection(HOST_ID, 20L);

        verify(deskStateMachine).markMaintenance(desk);
    }

    @Test
    void shouldUpdateAndDeleteAmenityPresetForHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        SpaceAmenityPreset preset = new SpaceAmenityPreset();
        preset.setPresetID(1L);
        preset.setLabel("WiFi");
        preset.setAmenities(List.of("WIFI"));

        SpaceAmenityPresetDTO body = new SpaceAmenityPresetDTO();
        body.setLabel("WiFi Plus");
        body.setHint("Suggerimento aggiornato");
        body.setAmenities(List.of("WIFI", "MONITOR"));

        SpaceAmenityPresetDTO dto = new SpaceAmenityPresetDTO();
        dto.setPresetID(1L);
        dto.setLabel("WiFi Plus");

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(amenityPresetRepo.findByPresetIDAndSpace_SpaceID(1L, SPACE_ID)).thenReturn(Optional.of(preset));
        when(amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(SPACE_ID)).thenReturn(List.of(preset));
        when(amenityPresetRepo.save(preset)).thenReturn(preset);
        when(spaceAmenityPresetMapper.toDto(preset)).thenReturn(dto);

        SpaceAmenityPresetDTO updated = hostAmenityPresetService.updateAmenityPresetForHost(HOST_ID, SPACE_ID, 1L, body);
        assertEquals("WiFi Plus", updated.getLabel());

        hostAmenityPresetService.deleteAmenityPresetForHost(HOST_ID, SPACE_ID, 1L);
        verify(spaceRepo).save(space);
    }

    @Test
    void shouldRejectDuplicateAmenityPresetLabel() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        SpaceAmenityPreset existing = new SpaceAmenityPreset();
        existing.setPresetID(1L);
        existing.setLabel("Configurazione monitor");
        existing.setAmenities(List.of("MONITOR"));

        SpaceAmenityPresetDTO body = new SpaceAmenityPresetDTO();
        body.setLabel("configurazione monitor");
        body.setHint("Duplicato");
        body.setAmenities(List.of("MONITOR"));

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID)).thenReturn(space);
        when(amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(SPACE_ID)).thenReturn(List.of(existing));

        assertThrows(BusinessRuleException.class,
                () -> hostAmenityPresetService.createAmenityPresetForHost(HOST_ID, SPACE_ID, body));
    }
}
