package it.polimi.smartdesk_backend.service.ticket;
import it.polimi.smartdesk_backend.mapper.DeskMapper;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianAssignedSpaceMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import lombok.RequiredArgsConstructor;

/** Assegnazioni tecnico↔spazio e controllo accesso su desk e ticket: operazioni consentite solo sugli spazi assegnati. */
@Service
@RequiredArgsConstructor
public class TechnicianAssignmentService {

    private final TechnicianRepository technicianRepository;
    private final DeskRepository deskRepository;
    private final TechnicianAssignedSpaceMapper technicianAssignedSpaceMapper;
    private final DeskStateMachine deskStateMachine;

    /** Spazi assegnati al tecnico, arricchiti e ordinati per nome. */
    @Transactional(readOnly = true)
    public List<TechnicianAssignedSpaceDTO> listAssignedSpaces(Long technicianUserId) {
        return technicianRepository
                .findById(technicianUserId)
                .map(this::mapSpaces)
                .orElse(List.of());
    }

    /** Desk dello spazio dopo verifica che sia tra quelli assegnati al tecnico; non filtra per stato macchina. */
    @Transactional(readOnly = true)
    public List<DeskDTO> listAssignedDesks(Long technicianUserId, Long spaceId) {
        assertAssignedToSpace(technicianUserId, spaceId);
        return deskRepository.findBySpaceSpaceID(spaceId).stream()
                .map(this::toDeskDto)
                .collect(Collectors.toList());
    }

    /** Prima di manutenzione desk: {@link NotFoundException} se lo spazio della postazione non è assegnato al tecnico (risposta uniforme). */
    @Transactional(readOnly = true)
    public void assertDeskAssignedToTechnician(Long technicianUserId, Long deskId) {
        Desk desk = deskRepository.findById(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        Long spaceId = desk.getSpace() == null ? null : desk.getSpace().getSpaceID();
        if (spaceId == null || !isAssignedToSpace(technicianUserId, spaceId)) {
            throw new NotFoundException(ResourceMessage.deskNotFound(deskId));
        }
    }

    /** Verifica assegnazione spazio; {@link NotFoundException} se non autorizzato (non distingue spazio inesistente da non assegnato). */
    private void assertAssignedToSpace(Long technicianUserId, Long spaceId) {
        if (!isAssignedToSpace(technicianUserId, spaceId)) {
            throw new NotFoundException(ResourceMessage.spaceNotFound(spaceId));
        }
    }

    /** {@code false} se il tecnico non esiste in tabella: niente eccezione, solo “non assegnato”. */
    private boolean isAssignedToSpace(Long technicianUserId, Long spaceId) {
        return technicianRepository.findById(technicianUserId)
                .map(technician -> technician.getSpaces().stream()
                        .anyMatch(space -> spaceId.equals(space.getSpaceID())))
                .orElse(false);
    }

    /** Ordinamento locale: la collection JPA non garantisce ordine stabile tra reload. */
    private List<TechnicianAssignedSpaceDTO> mapSpaces(Technician technician) {
        return technician.getSpaces().stream()
                .sorted(Comparator.comparing(Space::getName, String.CASE_INSENSITIVE_ORDER))
                .map(technicianAssignedSpaceMapper::toDto)
                .collect(Collectors.toList());
    }

    private DeskDTO toDeskDto(Desk desk) {
        return DeskMapper.fromDesk(desk);
    }
}

