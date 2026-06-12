package it.polimi.smartdesk_backend.service.host;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.SpaceAmenityPresetDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.SpaceAmenityPresetMapper;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.space.SpaceAmenityPreset;
import it.polimi.smartdesk_backend.repository.space.SpaceAmenityPresetRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.support.TextValidation;
import lombok.RequiredArgsConstructor;

/** Preset di amenity riutilizzabili per uno spazio (etichetta, token amenity, hint opzionale). Impedisce duplicati per etichetta o per insieme di amenity normalizzate. */
@Service
@RequiredArgsConstructor
public class HostAmenityPresetService {

    private static final int AMENITY_TOKEN_MAX_LEN = 12;

    private final SpaceRepository spaceRepo;
    private final SpaceAmenityPresetRepository amenityPresetRepo;
    private final SpaceAmenityPresetMapper spaceAmenityPresetMapper;
    private final HostOwnershipService hostOwnershipService;

    /** Elenco preset dello spazio, ordinati per etichetta. */
    @Transactional(readOnly = true)
    public List<SpaceAmenityPresetDTO> listAmenityPresetsForHost(Long hostID, Long spaceID) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        return amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(spaceID).stream()
                .map(spaceAmenityPresetMapper::toDto)
                .toList();
    }

    /**
     * Crea un preset sullo spazio dell'host; almeno un token amenity obbligatorio.
     *
     * @throws BusinessRuleException etichetta o set amenity già presenti, lista amenity vuota
     * @throws NotFoundException spazio non di proprietà
     */
    @Transactional
    public SpaceAmenityPresetDTO createAmenityPresetForHost(Long hostID, Long spaceID, SpaceAmenityPresetDTO body) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        String label = TextValidation.requireTrimmed(body.getLabel(), SpaceMessage.PRESET_LABEL_REQUIRED.text());
        List<String> amenitiesNorm = requireAmenityTokens(body.getAmenities());
        assertNoDuplicatePreset(spaceID, null, label, amenitiesNorm);
        SpaceAmenityPreset entity = new SpaceAmenityPreset();
        entity.setLabel(label);
        entity.setHint(TextValidation.requireTrimmed(body.getHint(), SpaceMessage.PRESET_HINT_REQUIRED.text()));
        entity.setAmenities(amenitiesNorm);
        space.addAmenityPreset(entity);
        spaceRepo.save(space);
        return spaceAmenityPresetMapper.toDto(entity);
    }

    /**
     * Aggiorna etichetta, hint e amenity; riesegue i controlli anti-duplicato escludendo il preset corrente.
     *
     * @throws NotFoundException preset assente nello spazio
     */
    @Transactional
    public SpaceAmenityPresetDTO updateAmenityPresetForHost(Long hostID, Long spaceID, Long presetID, SpaceAmenityPresetDTO body) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        SpaceAmenityPreset entity = amenityPresetRepo.findByPresetIDAndSpace_SpaceID(presetID, spaceID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.amenityPresetNotFound(presetID)));
        String label = TextValidation.requireTrimmed(body.getLabel(), SpaceMessage.PRESET_LABEL_REQUIRED.text());
        List<String> amenitiesNorm = requireAmenityTokens(body.getAmenities());
        assertNoDuplicatePreset(spaceID, presetID, label, amenitiesNorm);
        entity.setLabel(label);
        entity.setHint(TextValidation.requireTrimmed(body.getHint(), SpaceMessage.PRESET_HINT_REQUIRED.text()));
        entity.setAmenities(amenitiesNorm);
        amenityPresetRepo.save(entity);
        return spaceAmenityPresetMapper.toDto(entity);
    }

    /**
     * Rimuove il preset dall'aggregato spazio e persiste.
     *
     * @throws NotFoundException preset non trovato nello spazio
     */
    @Transactional
    public void deleteAmenityPresetForHost(Long hostID, Long spaceID, Long presetID) {
        Space space = hostOwnershipService.loadOwnedSpaceOrNotFound(hostID, spaceID);
        SpaceAmenityPreset entity = amenityPresetRepo.findByPresetIDAndSpace_SpaceID(presetID, spaceID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.amenityPresetNotFound(presetID)));
        space.removeAmenityPreset(entity);
        spaceRepo.save(space);
    }

    private List<String> requireAmenityTokens(List<String> raw) {
        List<String> normalized = normalizeAmenityTokens(raw);
        if (normalized.isEmpty()) {
            throw new BusinessRuleException(SpaceMessage.PRESET_MIN_ONE_AMENITY.text());
        }
        return normalized;
    }

    private List<String> normalizeAmenityTokens(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim().toUpperCase(Locale.ROOT);
            if (t.length() > AMENITY_TOKEN_MAX_LEN) {
                t = t.substring(0, AMENITY_TOKEN_MAX_LEN);
            }
            if (!t.isEmpty()) {
                seen.add(t);
            }
        }
        return new ArrayList<>(seen);
    }

    private String labelKey(String label) {
        return label.trim().toLowerCase(Locale.ROOT);
    }

    private String amenitySignature(List<String> normalized) {
        return normalized.stream()
                .sorted(Comparator.comparing(a -> a, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.joining("\u0001"));
    }

    private void assertNoDuplicatePreset(Long spaceId, Long excludePresetId, String label, List<String> amenitiesNorm) {
        String lk = labelKey(label);
        String sig = amenitySignature(amenitiesNorm);
        for (SpaceAmenityPreset p : amenityPresetRepo.findBySpace_SpaceIDOrderByLabelAsc(spaceId)) {
            if (excludePresetId != null && excludePresetId.equals(p.getPresetID())) {
                continue;
            }
            if (labelKey(p.getLabel()).equals(lk)) {
                throw new BusinessRuleException(SpaceMessage.PRESET_DUPLICATE_LABEL.text());
            }
            if (amenitySignature(p.getAmenities()).equals(sig)) {
                throw new BusinessRuleException(SpaceMessage.PRESET_DUPLICATE_AMENITIES.text());
            }
        }
    }
}

