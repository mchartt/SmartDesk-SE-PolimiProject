package it.polimi.smartdesk_backend.service.host;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import lombok.RequiredArgsConstructor;

/** Assegnazione tecnico ↔ spazio: link esplicito, lista per spazio, idempotenza su {@code ensure}. */
@Service
@RequiredArgsConstructor
public class HostTechnicianSpaceManagementService {

    private final TechnicianRepository technicianRepository;
    private final TechnicianMapper technicianMapper;
    private final HostOwnershipService hostOwnershipService;
    private final HostTechnicianAccessService hostTechnicianAccessService;

    /**
     * Collega un tecnico a uno spazio dell'host; il tecnico accede ai ticket di quello spazio.
     *
     * @param hostID ID dell'host proprietario dello spazio
     * @param spaceID ID dello spazio
     * @param technicianID ID del tecnico da assegnare
     * @return tecnico aggiornato con le nuove assegnazioni
     * @throws BusinessRuleException tecnico già assegnato allo spazio
     */
    @Transactional
    public TechnicianDTO assignTechnicianToSpace(Long hostID, Long spaceID, Long technicianID) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        Technician technician = loadTechnician(technicianID);
        if (hostTechnicianAccessService.technicianLinkedToSpace(technician, space.getSpaceID())) {
            throw new BusinessRuleException(SpaceMessage.TECHNICIAN_ALREADY_ASSIGNED.text());
        }
        technician.assignSpace(space);
        return technicianMapper.toDto(technicianRepository.save(technician));
    }

    /**
     * Rimuove il collegamento tra tecnico e spazio.
     *
     * @param hostID ID dell'host
     * @param spaceID ID dello spazio
     * @param technicianID ID del tecnico da de-assegnare
     * @throws NotFoundException tecnico non collegato a quello spazio
     */
    @Transactional
    public void unassignTechnicianFromSpace(Long hostID, Long spaceID, Long technicianID) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        Technician technician = loadTechnician(technicianID);
        if (!hostTechnicianAccessService.technicianLinkedToSpace(technician, space.getSpaceID())) {
            throw new NotFoundException(ResourceMessage.technicianNotFoundInSpace(technicianID, spaceID));
        }
        Long sid = space.getSpaceID();
        technician.getSpaces().removeIf(s -> Objects.equals(sid, s.getSpaceID()));
        technicianRepository.save(technician);
    }

    /** Tecnici attualmente assegnati allo spazio (solo se lo spazio è dell'host). */
    @Transactional(readOnly = true)
    public List<TechnicianDTO> getTechniciansForSpace(Long hostID, Long spaceID) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        return technicianRepository.findBySpaceID(spaceID).stream()
                .map(technicianMapper::toDto)
                .toList();
    }

    /** Assegna solo se manca il link; verifica prima che il tecnico sia gestibile dall'host. */
    @Transactional
    public void ensureTechnicianLinkedToSpace(Long hostID, Long spaceID, Long technicianID) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        Technician technician = loadTechnician(technicianID);
        hostTechnicianAccessService.assertTechnicianManagedByHost(hostID, technician);
        if (hostTechnicianAccessService.technicianLinkedToSpace(technician, spaceID)) {
            return;
        }
        technician.assignSpace(space);
        technicianRepository.save(technician);
    }

    private Technician loadTechnician(Long technicianID) {
        return technicianRepository.findById(technicianID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.technicianNotFound(technicianID)));
    }
}

