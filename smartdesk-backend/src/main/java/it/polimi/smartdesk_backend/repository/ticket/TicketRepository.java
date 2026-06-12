package it.polimi.smartdesk_backend.repository.ticket;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.polimi.smartdesk_backend.model.ticket.Ticket;

/** Accesso base ai ticket. Le regole su chi vede cosa stanno nei service/specification. */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByStatus(String status);

    List<Ticket> findByWorkerID(Long workerID);

    List<Ticket> findByTechnicianID(Long technicianID);

    List<Ticket> findByDeskID(Long deskID);

    boolean existsBySpaceIDAndTicketCode(Long spaceID, String ticketCode);

    boolean existsBySpaceIDIsNullAndTicketCode(String ticketCode);

    boolean existsByTechnicianIDAndStatusIn(Long technicianID, Iterable<String> statuses);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.status = 'OPEN'
            AND EXISTS (
                SELECT 1 FROM Technician tech JOIN tech.spaces s
                WHERE tech.id = :technicianID AND s.spaceID = t.spaceID
            )
            """)
    List<Ticket> findOpenTicketsInSpacesAssignedToTechnician(@Param("technicianID") Long technicianID);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.workerID = :workerID
            AND (t.status <> 'RESOLVED' OR t.resolvedAt IS NULL OR t.resolvedAt >= :cutoff)
            """)
    List<Ticket> findVisibleToWorker(@Param("workerID") Long workerID, @Param("cutoff") LocalDateTime cutoff, org.springframework.data.domain.Sort sort);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.technicianID = :technicianID
            AND (t.status <> 'RESOLVED' OR t.resolvedAt IS NULL OR t.resolvedAt >= :cutoff)
            """)
    List<Ticket> findVisibleToTechnician(@Param("technicianID") Long technicianID, @Param("cutoff") LocalDateTime cutoff, org.springframework.data.domain.Sort sort);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.status = 'RESOLVED'
            AND EXISTS (
                SELECT 1 FROM Desk d JOIN d.space s
                WHERE d.deskID = t.deskID AND s.hostID = :hostID
            )
            """)
    List<Ticket> findResolvedHistoryForHost(@Param("hostID") Long hostID);

    @Modifying
    @Query("""
            DELETE FROM Ticket t
            WHERE t.status = :status
            AND t.resolvedAt IS NOT NULL
            AND t.resolvedAt < :cutoff
            """)
    int deleteByStatusAndResolvedAtBefore(@Param("status") String status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Ticket t
            WHERE t.status = :status
            AND EXISTS (
                SELECT 1 FROM Desk d JOIN d.space s
                WHERE d.deskID = t.deskID AND s.hostID = :hostID
            )
            """)
    int deleteByStatusAndDeskHost(@Param("status") String status, @Param("hostID") Long hostID);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Ticket t
            WHERE t.technicianID = :technicianID
            AND t.status IN ('RESOLVED', 'CLOSED')
            """)
    int deleteResolvedHistoryForTechnician(@Param("technicianID") Long technicianID);
}

