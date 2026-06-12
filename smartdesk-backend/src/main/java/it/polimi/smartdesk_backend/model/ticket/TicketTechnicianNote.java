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

/** Singola nota lasciata dal tecnico su un ticket (storico append-only). */
@Entity
@Table(name = "ticket_technician_note")
@Getter
@Setter
@NoArgsConstructor
public class TicketTechnicianNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteID;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketID;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "technician_id")
    private Long technicianID;

    /** Factory: crea e valorizza una nota tecnico senza passare per il costruttore. */
    public static TicketTechnicianNote of(Long ticketID, Long technicianID, String body, LocalDateTime createdAt) {
        TicketTechnicianNote row = new TicketTechnicianNote();
        row.setTicketID(ticketID);
        row.setTechnicianID(technicianID);
        row.setBody(body);
        row.setCreatedAt(createdAt);
        return row;
    }
}
