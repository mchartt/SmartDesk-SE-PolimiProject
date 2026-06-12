package it.polimi.smartdesk_backend.model.ticket;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Commento aggiuntivo del worker su un ticket (storico append-only). */
@Entity
@Table(name = "ticket_worker_note")
@Getter
@Setter
@NoArgsConstructor
public class TicketWorkerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteID;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketID;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "worker_id")
    private Long workerID;

    /** Factory: crea e valorizza una nota worker senza passare per il costruttore. */
    public static TicketWorkerNote of(Long ticketID, Long workerID, String body, LocalDateTime createdAt) {
        TicketWorkerNote row = new TicketWorkerNote();
        row.setTicketID(ticketID);
        row.setWorkerID(workerID);
        row.setBody(body);
        row.setCreatedAt(createdAt);
        return row;
    }
}
