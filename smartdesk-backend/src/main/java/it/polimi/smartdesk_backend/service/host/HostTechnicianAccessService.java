package it.polimi.smartdesk_backend.service.host;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;

/** Delimita quali tecnici un host può gestire: creati da lui o assegnati ai suoi spazi. */
@Service
@RequiredArgsConstructor
public class HostTechnicianAccessService {

    private final SpaceRepository spaceRepo;

    /**
     * Verifica che il tecnico sia gestibile dall'host (creato da lui o assegnato ai suoi spazi).
     *
     * @throws NotFoundException tecnico non gestibile
     */
    @Transactional(readOnly = true)
    public void assertTechnicianManagedByHost(Long hostID, Technician technician) {
        if (hostID.equals(technician.getCreatingHostId())) {
            return;
        }
        Set<Long> hostSpaceIds = spaceRepo.findByHostID(hostID).stream()
                .map(Space::getSpaceID)
                .collect(Collectors.toSet());
        boolean overlaps = technician.getSpaces().stream().anyMatch(s -> hostSpaceIds.contains(s.getSpaceID()));
        if (!overlaps) {
            throw new NotFoundException(ResourceMessage.technicianNotFound(technician.getId()));
        }
    }

    /** {@code true} se il tecnico ha lo spazio nella propria collezione assegnata. */
    public boolean technicianLinkedToSpace(Technician technician, Long spaceId) {
        if (spaceId == null) {
            return false;
        }
        return technician.getSpaces().stream().anyMatch(s -> Objects.equals(spaceId, s.getSpaceID()));
    }
}

