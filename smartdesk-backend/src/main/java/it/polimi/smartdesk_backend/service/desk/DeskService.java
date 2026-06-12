package it.polimi.smartdesk_backend.service.desk;
import it.polimi.smartdesk_backend.mapper.DeskMapper;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;

/** Query catalogo desk e disponibilità giornaliera per worker. La occupazione temporale non è in {@code stateCode}: si incrocia con le prenotazioni attive. */
@Service
@RequiredArgsConstructor
public class DeskService {

    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final BookingRepository bookingRepo;
    private final DeskStateMachine deskStateMachine;

    /** Include desk in spazi non approvati; nessun filtro ownership. */
    @Transactional(readOnly = true)
    public List<DeskDTO> findAll() {
        return deskRepo.findAllWithSpaceAndRoom().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Catalogo pubblico: solo desk dentro spazi approvati. */
    @Transactional(readOnly = true)
    public List<DeskDTO> findAllApproved() {
        return deskRepo.findAllApprovedWithSpaceAndRoom().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Dettaglio singolo record desk; nessun check spazio approvato (chi chiama decide se è lecito). */
    @Transactional(readOnly = true)
    public DeskDTO findById(Long deskId) {
        Desk desk = deskRepo.findById(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        return toDTO(desk);
    }

    /** Dettaglio catalogo: desk non approvati o senza spazio risultano non trovati. */
    @Transactional(readOnly = true)
    public DeskDTO findApprovedById(Long deskId) {
        Desk desk = deskRepo.findById(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        if (!belongsToApprovedSpace(desk)) {
            throw new NotFoundException(ResourceMessage.deskNotFound(deskId));
        }
        return toDTO(desk);
    }

    /** Elenca i desk di uno spazio approvato; spazi non approvati restituiscono 404 (anti-enumeration). */
    @Transactional(readOnly = true)
    public List<DeskDTO> findBySpace(Long spaceId) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        if (!space.isApproved()) {
            throw new NotFoundException(ResourceMessage.spaceNotFound(spaceId));
        }
        return deskRepo.findBySpaceSpaceIDWithSpaceAndRoom(spaceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Desk dello spazio solo se {@code spaceId} esiste e appartiene a {@code hostId}; altrimenti stesso {@link NotFoundException} usato per ID inesistenti (anti-enumeration). */
    @Transactional(readOnly = true)
    public List<DeskDTO> findBySpaceForHost(Long hostId, Long spaceId) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        if (!hostId.equals(space.getHostID())) {
            throw new NotFoundException(ResourceMessage.spaceNotFound(spaceId));
        }
        return deskRepo.findBySpaceSpaceIDWithSpaceAndRoom(spaceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Elenca i desk prenotabili in una data, escludendo stati non bookable e prenotazioni attive; il DTO espone AVAILABLE. */
    @Transactional(readOnly = true)
    public List<DeskDTO> findAvailable(LocalDate date) {
        // Una postazione è idonea se il suo stato la considera prenotabile.
        // Escludiamo quelle con prenotazioni attive/non cancellate nel giorno richiesto.
        return deskRepo.findAllApprovedWithSpaceAndRoom().stream()
                .filter(deskStateMachine::isBookable)
                .filter(desk -> bookingRepo.findActiveNonCancelledOverlappingDay(desk.getDeskID(), date).isEmpty())
                .map(desk -> {
                    DeskDTO dto = toDTO(desk);
                    // Per questo endpoint la disponibilità calcolata prevale sullo stato interno del DTO.
                    dto.setCurrentState(DeskStateCode.AVAILABLE.name());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** Converte verso l'oggetto per le API. */
    private DeskDTO toDTO(Desk desk) {
        return DeskMapper.fromDesk(desk);
    }

    private boolean belongsToApprovedSpace(Desk desk) {
        return desk.getSpace() != null && desk.getSpace().isApproved();
    }
}

