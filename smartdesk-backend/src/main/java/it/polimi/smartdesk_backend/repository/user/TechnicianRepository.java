package it.polimi.smartdesk_backend.repository.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.polimi.smartdesk_backend.model.user.Technician;

/** Tecnici per specializzazione e join con spazi assegnati (dashboard host). */
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findBySpecialization(String specialization);

    @Query("""
            SELECT t
            FROM Technician t
            JOIN t.spaces s
            WHERE s.spaceID = :spaceID
            """)
    List<Technician> findBySpaceID(Long spaceID);

    @Query("""
            SELECT DISTINCT t
            FROM Technician t
            LEFT JOIN FETCH t.spaces s
            WHERE t.creatingHostId = :hostID
            OR s.hostID = :hostID
            """)
    List<Technician> findForHostDashboard(@Param("hostID") Long hostID);
}

