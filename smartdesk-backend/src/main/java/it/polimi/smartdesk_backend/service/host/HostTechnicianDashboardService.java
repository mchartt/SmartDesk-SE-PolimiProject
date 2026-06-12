package it.polimi.smartdesk_backend.service.host;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianAssignedSpaceMapper;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import lombok.RequiredArgsConstructor;

/** Vista aggregata tecnici per la dashboard host: filtra gli spazi assegnati a quelli di proprietà dell'host e ordina per data registrazione. */
@Service
@RequiredArgsConstructor
public class HostTechnicianDashboardService {

    private final SpaceRepository spaceRepo;
    private final HostRepository hostRepo;
    private final TechnicianRepository technicianRepository;
    private final TechnicianMapper technicianMapper;
    private final TechnicianAssignedSpaceMapper technicianAssignedSpaceMapper;

    /**
     * Elenca i tecnici visibili all'host con spazi assegnati filtrati ai soli spazi di proprietà.
     *
     * @throws NotFoundException host inesistente
     */
    @Transactional(readOnly = true)
    public List<TechnicianDTO> getTechniciansForHost(Long hostID) {
        hostRepo.findById(hostID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.hostNotFound(hostID)));
        Set<Long> hostSpaceIds = hostSpaceIds(hostID);
        return technicianRepository.findForHostDashboard(hostID).stream()
                .map(t -> toTechnicianDTOForHostDashboard(t, hostSpaceIds))
                .sorted(Comparator
                        .comparing(TechnicianDTO::getRegisteredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TechnicianDTO::getTechnicianID, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Mappa un tecnico già caricato usando gli spazi dell'host indicato. */
    public TechnicianDTO toTechnicianDTOForHostDashboard(Technician technician, Long hostID) {
        return toTechnicianDTOForHostDashboard(technician, hostSpaceIds(hostID));
    }

    private Set<Long> hostSpaceIds(Long hostID) {
        return spaceRepo.findByHostID(hostID).stream()
                .map(Space::getSpaceID)
                .collect(Collectors.toSet());
    }

    private TechnicianDTO toTechnicianDTOForHostDashboard(Technician technician, Set<Long> hostSpaceIds) {
        TechnicianDTO dto = technicianMapper.toDto(technician);
        List<TechnicianAssignedSpaceDTO> assigned = technician.getSpaces().stream()
                .filter(s -> hostSpaceIds.contains(s.getSpaceID()))
                .sorted(Comparator.comparing(Space::getName, String.CASE_INSENSITIVE_ORDER))
                .map(technicianAssignedSpaceMapper::toDto)
                .toList();
        dto.setAssignedSpaces(assigned);
        return dto;
    }
}

