package it.polimi.smartdesk_backend.repository.space;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.space.Space;

/** Persistenza spazi: lookup per host, approvazione e catalogo admin. */
public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByHostID(Long hostID);

    List<Space> findByApprovedTrue();

    List<Space> findByApprovedFalse();

    boolean existsBySpaceIDAndHostID(Long spaceID, Long hostID);

    boolean existsByOfficeCode(String officeCode);
}

