package it.polimi.smartdesk_backend.repository.space;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.space.Room;

/** Stanze per sede: lookup per codice univoco nello spazio e liste ordinate per UI host. */
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findBySpace_SpaceIDOrderByNameAsc(Long spaceID);

    /** Codice stanza è univoco per {@code spaceID}, non globalmente. */
    Optional<Room> findBySpace_SpaceIDAndCode(Long spaceID, String code);

    boolean existsBySpace_SpaceIDAndCode(Long spaceID, String code);
}

