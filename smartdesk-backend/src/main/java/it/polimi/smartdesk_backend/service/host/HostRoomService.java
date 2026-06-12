package it.polimi.smartdesk_backend.service.host;

import it.polimi.smartdesk_backend.util.space.RoomDeskCodeSupport;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.RoomDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.RoomMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Room;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.RoomRepository;
import it.polimi.smartdesk_backend.support.TextValidation;
import lombok.RequiredArgsConstructor;

/** Stanze (room) dentro uno spazio: codice 2–10 caratteri alfanumerici maiuscoli, univoco per spazio. Al rename propaga il nome stanza sui desk collegati come campo {@code building}. */
@Service
@RequiredArgsConstructor
public class HostRoomService {

    private static final String ROOM_CODE_PATTERN = "^[A-Z0-9]{2,10}$";

    private final DeskRepository deskRepo;
    private final RoomRepository roomRepo;
    private final RoomMapper roomMapper;
    private final HostOwnershipService hostOwnershipService;

    /** Stanze dello spazio ordinate per nome. */
    @Transactional(readOnly = true)
    public List<RoomDTO> listRoomsForHost(Long hostID, Long spaceID) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        return roomRepo.findBySpace_SpaceIDOrderByNameAsc(spaceID).stream()
                .map(roomMapper::toDto)
                .toList();
    }

    /**
     * Crea una stanza nello spazio dell'host con codice alfanumerico univoco.
     *
     * @throws BusinessRuleException codice già usato o formato non valido
     */
    @Transactional
    public RoomDTO createRoomForHost(Long hostID, Long spaceID, RoomDTO body) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        String name = TextValidation.requireTrimmed(body.getName(), SpaceMessage.ROOM_NAME_REQUIRED.text());
        String code = normalizeRoomCode(body.getCode());
        assertRoomCodeFormat(code);
        if (roomRepo.existsBySpace_SpaceIDAndCode(spaceID, code)) {
            throw new BusinessRuleException(SpaceMessage.ROOM_CODE_IN_USE.text());
        }
        Room room = new Room();
        room.setSpace(space);
        room.setName(name);
        room.setCode(code);
        return roomMapper.toDto(roomRepo.save(room));
    }

    /**
     * Aggiorna nome/codice e sincronizza {@code building} sui desk della stanza.
     *
     * @throws NotFoundException stanza fuori dallo spazio
     */
    @Transactional
    public RoomDTO updateRoomForHost(Long hostID, Long spaceID, Long roomID, RoomDTO body) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        Room room = roomInSpace(spaceID, roomID);
        String name = TextValidation.requireTrimmed(body.getName(), SpaceMessage.ROOM_NAME_REQUIRED.text());
        String code = normalizeRoomCode(body.getCode());
        assertRoomCodeFormat(code);
        if (!code.equals(room.getCode()) && roomRepo.existsBySpace_SpaceIDAndCode(spaceID, code)) {
            throw new BusinessRuleException(SpaceMessage.ROOM_CODE_IN_USE.text());
        }
        String previousCode = room.getCode();
        boolean codeChanged = !code.equals(previousCode);
        room.setName(name);
        room.setCode(code);
        Room saved = roomRepo.save(room);
        List<Desk> desks = deskRepo.findByRoom_RoomID(roomID);
        for (Desk desk : desks) {
            desk.setBuilding(saved.getName());
        }
        if (codeChanged && !desks.isEmpty()) {
            RoomDeskCodeSupport.renumberDesksInRoom(previousCode, saved.getCode(), desks, spaceID, deskRepo);
        }
        for (Desk desk : desks) {
            deskRepo.save(desk);
        }
        return roomMapper.toDto(saved);
    }

    /**
     * Elimina la stanza solo se non ci sono desk associati.
     *
     * @throws BusinessRuleException stanza ancora con desk
     */
    @Transactional
    public void deleteRoomForHost(Long hostID, Long spaceID, Long roomID) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        Room room = roomInSpace(spaceID, roomID);
        if (deskRepo.countByRoom_RoomID(roomID) > 0) {
            throw new BusinessRuleException(SpaceMessage.ROOM_DELETE_HAS_DESKS.text());
        }
        roomRepo.delete(room);
    }

    private Room roomInSpace(Long spaceId, Long roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.roomNotFound(roomId)));
        if (!spaceId.equals(room.getSpace().getSpaceID())) {
            throw new NotFoundException(ResourceMessage.roomNotFound(roomId));
        }
        return room;
    }

    private static String normalizeRoomCode(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase();
    }

    private static void assertRoomCodeFormat(String code) {
        if (!code.matches(ROOM_CODE_PATTERN)) {
            throw new BusinessRuleException(SpaceMessage.ROOM_CODE_FORMAT.text());
        }
    }
}

