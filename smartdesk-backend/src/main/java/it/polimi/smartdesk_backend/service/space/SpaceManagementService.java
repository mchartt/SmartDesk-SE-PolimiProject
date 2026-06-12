package it.polimi.smartdesk_backend.service.space;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.mapper.SpaceMapper;
import it.polimi.smartdesk_backend.dto.space.SpaceRequestDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ConflictException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.admin.SysAdminNotificationService;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.support.codegen.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

/** Ciclo di vita degli spazi coworking: catalogo pubblico, CRUD host, approvazione admin, orari di apertura e validazione prenotazioni dentro la finestra operativa. */
@Service
@RequiredArgsConstructor
public class SpaceManagementService implements SpaceOperations {

    private final SpaceRepository spaceRepo;
    private final SysAdminNotificationService sysAdminNotificationService;
    private final ReviewStatsService reviewStatsService;
    private final HostOwnershipService hostOwnershipService;
    private final OpeningHoursService openingHoursService;
    private final SpaceMapper spaceMapper;
    private final SpaceClosureRepository spaceClosureRepository;

    /** Catalogo pubblico: solo spazi approvati con rating medio. */
    @Override
    @Transactional(readOnly = true)
    public List<SpaceDTO> findAll() {
        Map<Long, Double> avg = reviewStatsService.averageRatingBySpaceId();
        return spaceRepo.findByApprovedTrue().stream()
                .map(space -> enrich(space, spaceMapper.toDto(space), avg.get(space.getSpaceID()), null))
                .collect(Collectors.toList());
    }

    /**
     * Dettaglio spazio approvato; 404 se pending o id inesistente.
     *
     * @throws NotFoundException spazio non trovato o non ancora approvato
     */
    @Override
    @Transactional(readOnly = true)
    public SpaceDTO findById(Long spaceId) {
        Space space = loadSpace(spaceId);
        if (!space.isApproved()) {
            throw new NotFoundException(ResourceMessage.spaceNotFound(spaceId));
        }
        Map<Long, Double> avg = reviewStatsService.averageRatingBySpaceId();
        return enrich(space, spaceMapper.toDto(space), avg.get(space.getSpaceID()), null);
    }

    /** Tutti gli spazi di un host (anche non approvati), con rating se disponibile. */
    @Override
    @Transactional(readOnly = true)
    public List<SpaceDTO> findByHost(Long hostId) {
        Map<Long, Double> avg = reviewStatsService.averageRatingBySpaceId();
        return spaceRepo.findByHostID(hostId).stream()
                .map(space -> enrich(space, spaceMapper.toDto(space), avg.get(space.getSpaceID()), null))
                .collect(Collectors.toList());
    }

    /**
     * Verifica che lo spazio appartenga all'host.
     *
     * @throws NotFoundException spazio assente o di un altro host
     */
    @Transactional(readOnly = true)
    public void assertHostOwnsSpace(Long hostId, Long spaceId) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
    }

    /**
     * L'host crea una sede: parte non approvata, con codice ufficio univoco e notifica agli admin.
     *
     * @throws BusinessRuleException host non ancora approvato
     * @throws ConflictException collisione su office code
     */
    @Transactional
    public SpaceDTO createSpace(Long hostId, SpaceRequestDTO request) {
        Host host = hostOwnershipService.loadHostOrNotFound(hostId);
        if (!host.isApproved()) {
            throw new BusinessRuleException(SpaceMessage.HOST_NOT_APPROVED.text());
        }

        Space space = new Space();
        space.setHostID(hostId);
        copyEditableFields(space, request);
        space.setApproved(false);
        space.setOfficeCode(CodeUtils.allocateUniqueCode(spaceRepo::existsByOfficeCode, 10, "OFFICE_CODE"));
        openingHoursService.applyFromRequest(space, request.getOpeningHours());

        try {
            Space saved = spaceRepo.save(space);
            sysAdminNotificationService.notifyAdminsOfPendingSpace(saved, host);
            return enrich(saved, spaceMapper.toDto(saved), null, host);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(SpaceMessage.OFFICE_CODE_CONFLICT.text());
        }
    }

    /** Aggiorna campi editabili e orari di apertura (uso interno/admin). */
    @Transactional
    public SpaceDTO updateSpace(Long spaceId, SpaceRequestDTO request) {
        Space space = loadSpace(spaceId);
        copyEditableFields(space, request);
        openingHoursService.applyFromRequest(space, request.getOpeningHours());
        return enrich(space, spaceMapper.toDto(spaceRepo.save(space)), null, null);
    }

    /** Come {@link #updateSpace} ma controlla prima la proprietà host. */
    @Transactional
    public SpaceDTO updateSpaceForHost(Long hostId, Long spaceId, SpaceRequestDTO request) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
        return updateSpace(spaceId, request);
    }

    /** Elimina lo spazio per id (admin). */
    @Transactional
    public void deleteSpace(Long spaceId) {
        spaceRepo.delete(loadSpace(spaceId));
    }

    /** Elimina lo spazio solo se appartiene all'host del path. */
    @Transactional
    public void deleteSpaceForHost(Long hostId, Long spaceId) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
        spaceRepo.delete(space);
    }

    /** Deserializza gli orari di apertura nel DTO di risposta. */
    public void enrichOpeningHours(Space space, SpaceDTO dto) {
        openingHoursService.enrichDto(space, dto);
    }

    /**
     * Delega a {@link OpeningHoursService#assertBookingWithinOpeningHours}.
     *
     * @throws BusinessRuleException fascia fuori dagli orari di apertura
     */
    public void assertBookingWithinOpeningHours(Space space, LocalDateTime start, LocalDateTime end) {
        openingHoursService.assertBookingWithinOpeningHours(space, start, end);
    }

    /**
     * Verifica assenza di chiusura straordinaria nella data indicata.
     *
     * @throws BusinessRuleException sede chiusa nella data indicata
     */
    public void assertSpaceOpenOnCalendarDay(Space space, LocalDate day) {
        if (space == null || day == null) {
            return;
        }
        if (spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(space.getSpaceID(), day)) {
            throw new BusinessRuleException(SpaceMessage.SPACE_CLOSED_ON_SELECTED_DATE.text());
        }
    }

    private Space loadSpace(Long spaceId) {
        return spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
    }

    /** Arricchisce il DTO con rating medio, orari di apertura e dati host. */
    public SpaceDTO enrich(Space space, SpaceDTO dto, Double avgRating, Host host) {
        dto.setAverageReviewRating(avgRating);
        openingHoursService.enrichDto(space, dto);
        if (host != null) {
            spaceMapper.applyHostFields(dto, host);
        } else if (space.getHostID() != null) {
            try {
                Host h = hostOwnershipService.loadHostOrNotFound(space.getHostID());
                spaceMapper.applyHostFields(dto, h);
            } catch (NotFoundException ignored) {}
        }
        return dto;
    }

    private static void copyEditableFields(Space space, SpaceRequestDTO request) {
        space.setName(request.getName());
        space.setDescription(request.getDescription());
        space.setAddress(request.getAddress());
        space.setCity(request.getCity());
    }
}
