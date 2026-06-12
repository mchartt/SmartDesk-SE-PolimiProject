package it.polimi.smartdesk_backend.repository.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.polimi.smartdesk_backend.model.notification.Notification;

/** Inbox persistita per destinatario; update bulk e delete solo righe già lette. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop200ByRecipientIDOrderByCreatedAtDesc(Long recipientID);

    long countByRecipientIDAndReadFalse(Long recipientID);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientID = :recipientID AND n.read = false")
    int markAllAsRead(@Param("recipientID") Long recipientID);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientID = :recipientID AND n.read = true")
    int deleteReadHistory(@Param("recipientID") Long recipientID);
}

