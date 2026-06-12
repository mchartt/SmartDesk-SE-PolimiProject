package it.polimi.smartdesk_backend.service.admin;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.mapper.SpaceMapper;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Viste spazi per sys admin: catalogo completo, approvati arricchiti o coda di approvazione. */
@Service
@RequiredArgsConstructor
public class AdminSpaceService implements AdminSpaceOperations {

    private final SpaceRepository spaceRepo;
    private final ReviewStatsService reviewStatsService;
    private final HostOwnershipService hostOwnershipService;
    private final SpaceMapper spaceMapper;
    private final SpaceManagementService spaceManagementService;

    /** Tutti gli spazi del sistema, senza filtri (vista admin). */
    @Override
    @Transactional(readOnly = true)
    public List<SpaceDTO> findAllForAdmin() {
        return spaceRepo.findAll().stream()
                .map(space -> spaceManagementService.enrich(space, spaceMapper.toDto(space), null, null))
                .collect(Collectors.toList());
    }

    /** Lista degli spazi approvati con rating, dati host e ordinamento per città/nome. */
    @Override
    @Transactional(readOnly = true)
    public List<SpaceDTO> findApprovedEnrichedForAdmin() {
        Map<Long, Double> avgBySpaceId = reviewStatsService.averageRatingBySpaceId();
        List<Space> spaces = spaceRepo.findByApprovedTrue();

        Set<Long> hostIds = spaces.stream()
                .map(Space::getHostID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Host> hostMap = hostOwnershipService.findAllByIds(hostIds);

        return spaces.stream()
                .sorted(Comparator
                        .comparing(Space::getCity, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Space::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(space -> spaceManagementService.enrich(space, spaceMapper.toDto(space), 
                        avgBySpaceId.get(space.getSpaceID()), hostMap.get(space.getHostID())))
                .collect(Collectors.toList());
    }

    /** Coda approvazione: spazi con {@code approved=false}. */
    @Override
    @Transactional(readOnly = true)
    public List<SpaceDTO> findPendingApprovalForAdmin() {
        return spaceRepo.findByApprovedFalse().stream()
                .map(space -> spaceManagementService.enrich(space, spaceMapper.toDto(space), null, null))
                .collect(Collectors.toList());
    }
}
