package it.polimi.smartdesk_backend.service.host;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/** Verifica che host, spazi e desk appartengano alla stessa catena di proprietà. Gli errori di ownership sono sempre mascherati come 404 per non rivelare risorse altrui. */
@Service
@RequiredArgsConstructor
public class HostOwnershipService {

    private final HostRepository hostRepo;
    private final SpaceRepository spaceRepo;
    private final DeskRepository deskRepo;

    /**
     * Carica l'host o fallisce con messaggio standardizzato.
     *
     * @throws NotFoundException host assente
     */
    @Transactional(readOnly = true)
    public Host loadHostOrNotFound(Long hostId) {
        return hostRepo.findById(hostId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.hostNotFound(hostId)));
    }

    /** Carica più host in un'unica query per batch lookup. */
    @Transactional(readOnly = true)
    public Map<Long, Host> findAllByIds(Collection<Long> ids) {
        return hostRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Host::getId, h -> h));
    }

    /**
     * Restituisce lo spazio solo se {@code hostId} coincide con {@code space.getHostID()}.
     *
     * @throws NotFoundException host, spazio assente o non di proprietà
     */
    @Transactional(readOnly = true)
    public Space loadOwnedSpaceOrNotFound(Long hostId, Long spaceId) {
        loadHostOrNotFound(hostId);
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        if (!hostId.equals(space.getHostID())) {
            throw new NotFoundException(ResourceMessage.spaceNotFound(spaceId));
        }
        return space;
    }

    /**
     * Verifica l'ownership del desk caricandolo dal repository.
     *
     * @throws NotFoundException desk assente o di altro host
     */
    @Transactional(readOnly = true)
    public void assertDeskOwnedByHostOrNotFound(Long hostId, Long deskId) {
        Desk desk = deskRepo.findById(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        assertDeskOwnedByHostOrNotFound(hostId, desk, deskId);
    }

    /** Stessi controlli di ownership su entità già caricate in memoria. */
    public void assertDeskOwnedByHostOrNotFound(Long hostId, Desk desk, Long deskId) {
        Long ownerHostId = desk.getSpace() == null ? null : desk.getSpace().getHostID();
        if (!hostId.equals(ownerHostId)) {
            throw new NotFoundException(ResourceMessage.deskNotFound(deskId));
        }
    }
}

