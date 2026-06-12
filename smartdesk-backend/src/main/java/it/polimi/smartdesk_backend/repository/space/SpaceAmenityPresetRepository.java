package it.polimi.smartdesk_backend.repository.space;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.space.SpaceAmenityPreset;

/** Template di servizi riutilizzabili quando si creano postazioni in una sede. */
public interface SpaceAmenityPresetRepository extends JpaRepository<SpaceAmenityPreset, Long> {

    List<SpaceAmenityPreset> findBySpace_SpaceIDOrderByLabelAsc(Long spaceID);

    /** Vincolo anti-IDOR: il preset deve appartenere allo spazio del path. */
    Optional<SpaceAmenityPreset> findByPresetIDAndSpace_SpaceID(Long presetID, Long spaceID);
}

