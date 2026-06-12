package it.polimi.smartdesk_backend.service.host;
import it.polimi.smartdesk_backend.mapper.DeskMapper;

import it.polimi.smartdesk_backend.util.space.RoomDeskCodeSupport;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.DeskRequestDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Room;
import it.polimi.smartdesk_backend.model.space.Space;
import java.util.List;

import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.RoomRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import lombok.RequiredArgsConstructor;

/** CRUD desk lato host: codice alfanumerico univico per spazio, stanza obbligatoria, prezzo fisso a zero. */
@Service
@RequiredArgsConstructor
public class HostDeskService {

    private static final String DESK_CODE_PATTERN = "^[A-Za-z][A-Za-z0-9]{0,15}$";

    private final SpaceRepository spaceRepo;
    private final DeskRepository deskRepo;
    private final RoomRepository roomRepo;
    private final HostOwnershipService hostOwnershipService;
    private final DeskStateMachine deskStateMachine;

    /**
     * Crea un nuovo desk nello spazio dell'host; la stanza deve appartenere allo stesso spazio.
     *
     * @throws BusinessRuleException codice malformato o già in uso, roomId mancante
     * @throws NotFoundException spazio/room non trovati o non coerenti
     */
    @Transactional
    public DeskDTO createDesk(Long hostID, DeskRequestDTO request) {
        if (request.getSpaceID() == null) {
            throw new NotFoundException(SpaceMessage.SPACE_ID_REQUIRED.text());
        }
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, request.getSpaceID());
        if (request.getRoomID() == null) {
            throw new BusinessRuleException(SpaceMessage.ROOM_ID_REQUIRED.text());
        }
        Room room = roomRepo.findById(request.getRoomID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.roomNotFound(request.getRoomID())));
        if (!space.getSpaceID().equals(room.getSpace().getSpaceID())) {
            throw new NotFoundException(ResourceMessage.roomNotFound(request.getRoomID()));
        }

        List<Desk> desksInRoom = deskRepo.findByRoom_RoomID(room.getRoomID());
        String code = RoomDeskCodeSupport.nextDeskCode(room.getCode(), desksInRoom);
        assertDeskCodeFormat(code);
        if (deskRepo.findBySpace_SpaceIDAndCode(space.getSpaceID(), code).isPresent()) {
            throw new BusinessRuleException(SpaceMessage.DESK_CODE_IN_USE.text());
        }

        Desk desk = new Desk();
        desk.setCode(code);
        desk.setBuilding(room.getName());
        desk.setRoom(room);
        desk.setAmenities(request.getAmenities());
        desk.setPricePerHour(0.0);
        space.addDesk(desk);

        return DeskMapper.fromDesk(deskRepo.save(desk));
    }

    /** Aggiorna codice, stanza e amenity senza controllo host (uso interno). */
    @Transactional
    public DeskDTO updateDesk(Long deskID, DeskRequestDTO request) {
        Desk desk = loadDesk(deskID);

        if (request.getRoomID() != null) {
            Room room = roomRepo.findById(request.getRoomID())
                    .orElseThrow(() -> new NotFoundException(ResourceMessage.roomNotFound(request.getRoomID())));
            if (!desk.getSpace().getSpaceID().equals(room.getSpace().getSpaceID())) {
                throw new NotFoundException(ResourceMessage.roomNotFound(request.getRoomID()));
            }
            Long currentRoomId = desk.getRoom() == null ? null : desk.getRoom().getRoomID();
            if (!room.getRoomID().equals(currentRoomId)) {
                List<Desk> desksInTargetRoom = deskRepo.findByRoom_RoomID(room.getRoomID()).stream()
                        .filter(other -> !other.getDeskID().equals(desk.getDeskID()))
                        .toList();
                String code = RoomDeskCodeSupport.nextDeskCode(room.getCode(), desksInTargetRoom);
                assertDeskCodeFormat(code);
                deskRepo.findBySpace_SpaceIDAndCode(desk.getSpace().getSpaceID(), code)
                        .filter(other -> !other.getDeskID().equals(desk.getDeskID()))
                        .ifPresent(other -> {
                            throw new BusinessRuleException(SpaceMessage.DESK_CODE_IN_USE.text());
                        });
                desk.setCode(code);
            }
            desk.setRoom(room);
            desk.setBuilding(room.getName());
        }

        desk.setAmenities(request.getAmenities());
        if (request.getPricePerHour() != null) {
            desk.setPricePerHour(request.getPricePerHour());
        }
        return DeskMapper.fromDesk(deskRepo.save(desk));
    }

    /** Come {@link #updateDesk} con verifica ownership host. */
    @Transactional
    public DeskDTO updateDeskForHost(Long hostID, Long deskID, DeskRequestDTO request) {
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, loadDesk(deskID), deskID);
        return updateDesk(deskID, request);
    }

    /** Elimina il desk dall'aggregato spazio se presente, altrimenti delete diretto. */
    @Transactional
    public void removeDesk(Long deskID) {
        Desk desk = loadDesk(deskID);
        Space space = desk.getSpace();
        if (space != null) {
            space.removeDesk(desk);
            spaceRepo.save(space);
            return;
        }
        deskRepo.delete(desk);
    }

    /** {@link #removeDesk} con verifica ownership. */
    @Transactional
    public void removeDeskForHost(Long hostID, Long deskID) {
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, loadDesk(deskID), deskID);
        removeDesk(deskID);
    }

    /**
     * Transizione PENDING_INSPECTION → AVAILABLE: l'host approva il ripristino dopo manutenzione.
     *
     * @throws NotFoundException desk assente o non di proprietà dell'host
     */
    @Transactional
    public DeskDTO approveInspection(Long hostID, Long deskID) {
        Desk desk = loadDesk(deskID);
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, desk, deskID);
        deskStateMachine.completeInspection(desk);
        return DeskMapper.fromDesk(deskRepo.save(desk));
    }

    /**
     * Transizione verso DECOMMISSIONED: dismissione definitiva della postazione da parte dell'host.
     *
     * @throws NotFoundException desk assente o non di proprietà dell'host
     */
    @Transactional
    public DeskDTO decommissionDesk(Long hostID, Long deskID) {
        Desk desk = loadDesk(deskID);
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, desk, deskID);
        deskStateMachine.decommission(desk);
        return DeskMapper.fromDesk(deskRepo.save(desk));
    }

    /** PENDING_INSPECTION → MAINTENANCE: ispezione non superata, nuovo intervento. */
    @Transactional
    public DeskDTO rejectInspection(Long hostID, Long deskID) {
        Desk desk = loadDesk(deskID);
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, desk, deskID);
        deskStateMachine.markMaintenance(desk);
        return DeskMapper.fromDesk(deskRepo.save(desk));
    }

    private Desk loadDesk(Long deskId) {
        return deskRepo.findById(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
    }

    private static void assertDeskCodeFormat(String code) {
        if (code.isEmpty() || !code.matches(DESK_CODE_PATTERN)) {
            throw new BusinessRuleException(SpaceMessage.DESK_CODE_FORMAT.text());
        }
    }
}

