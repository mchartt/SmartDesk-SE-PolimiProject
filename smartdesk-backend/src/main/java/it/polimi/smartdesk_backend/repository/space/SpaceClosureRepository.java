package it.polimi.smartdesk_backend.repository.space;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.space.SpaceClosure;

/** Giorni in cui la sede non accetta prenotazioni (festività, lavori, ecc.). */
public interface SpaceClosureRepository extends JpaRepository<SpaceClosure, Long> {

    List<SpaceClosure> findBySpace_SpaceIDOrderByClosedDateAsc(Long spaceId);

    /** Usato da {@link it.polimi.smartdesk_backend.util.policy.BookingTimeRules} prima di confermare una data. */
    boolean existsBySpace_SpaceIDAndClosedDate(Long spaceId, LocalDate closedDate);

    Optional<SpaceClosure> findBySpace_SpaceIDAndClosedDate(Long spaceId, LocalDate closedDate);
}

