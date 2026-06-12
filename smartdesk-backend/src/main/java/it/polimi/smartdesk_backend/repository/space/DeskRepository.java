package it.polimi.smartdesk_backend.repository.space;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;

/** Persistenza desk con fetch spazio/stanza e lock pessimistico per transazioni di prenotazione. Il codice postazione è univoco per {@code space_id}, non globalmente. */
public interface DeskRepository extends JpaRepository<Desk, Long> {

    List<Desk> findByStateCode(DeskStateCode stateCode);

    @Query("""
            SELECT d FROM Desk d
            LEFT JOIN FETCH d.space
            LEFT JOIN FETCH d.room
            WHERE d.space.spaceID = :spaceId
            """)
    List<Desk> findBySpaceSpaceIDWithSpaceAndRoom(@Param("spaceId") Long spaceId);

    List<Desk> findBySpaceSpaceID(Long spaceID);

    /** Cerca desk per codice case-insensitive; il codice è univoco per spazio, non globalmente. */
    List<Desk> findByCodeIgnoreCase(String code);

    Optional<Desk> findBySpace_SpaceIDAndCode(Long spaceId, String code);

    long countByRoom_RoomID(Long roomID);

    List<Desk> findByRoom_RoomID(Long roomID);

    @Query("SELECT d.space.spaceID FROM Desk d WHERE d.deskID = :deskId")
    Optional<Long> findSpaceIdByDeskID(@Param("deskId") Long deskId);

    @Query("""
            SELECT d FROM Desk d
            LEFT JOIN FETCH d.space
            LEFT JOIN FETCH d.room
            """)
    List<Desk> findAllWithSpaceAndRoom();

    @Query("""
            SELECT d FROM Desk d
            JOIN FETCH d.space s
            LEFT JOIN FETCH d.room
            WHERE s.approved = true
            """)
    List<Desk> findAllBySpaceApprovedTrue();

    @Query("""
            SELECT d FROM Desk d
            LEFT JOIN FETCH d.space
            LEFT JOIN FETCH d.room
            WHERE d.deskID IN :deskIds
            """)
    List<Desk> findAllWithSpaceAndRoomByDeskIdIn(@Param("deskIds") Collection<Long> deskIds);

    @Query("""
            SELECT d FROM Desk d
            LEFT JOIN FETCH d.space s
            LEFT JOIN FETCH d.room
            WHERE s.approved = true
            AND d.deskID IN :deskIds
            """)
    List<Desk> findAllApprovedWithSpaceAndRoomByDeskIdIn(@Param("deskIds") Collection<Long> deskIds);

    @Query("""
            SELECT d FROM Desk d
            JOIN FETCH d.space s
            LEFT JOIN FETCH d.room
            WHERE s.approved = true
            """)
    List<Desk> findAllApprovedWithSpaceAndRoom();

    /** Blocca la riga {@code desk} fino a commit/rollback della transazione corrente ({@code FOR UPDATE}), serializzando creazione/riprogrammazione/cancellazione prenotazioni sullo stesso desk tra tutte le istanze JVM. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Desk d WHERE d.deskID = :deskId")
    Optional<Desk> lockByDeskIdForUpdate(@Param("deskId") Long deskId);
}

