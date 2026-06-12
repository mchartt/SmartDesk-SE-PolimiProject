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

/** Nota lasciata dall'host su un ticket (es. feedback su riparazione o motivo rifiuto). */
@Entity
@Table(name = "ticket_host_note")
@Getter
@Setter
@NoArgsConstructor
public class TicketHostNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteID;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketID;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "host_id")
    private Long hostID;

    /** Factory: crea e valorizza una nota host senza passare per il costruttore. */
    public static TicketHostNote of(Long ticketID, Long hostID, String body, LocalDateTime createdAt) {
        TicketHostNote row = new TicketHostNote();
        row.setTicketID(ticketID);
        row.setHostID(hostID);
        row.setBody(body);
        row.setCreatedAt(createdAt);
        return row;
    }
}
