package it.polimi.smartdesk_backend.model.ticket;
import it.polimi.smartdesk_backend.model.space.Desk;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Ticket manutenzione su desk/spazio: stato OPEN/IN_PROGRESS/RESOLVED, codice univoco per spazio, note e risoluzione. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_ticket_space_code", columnNames = { "space_id", "ticket_code" }))
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketID;

    /** Spazio associato al desk del ticket. */
    @Column(name = "space_id")
    private Long spaceID;

    /** Codice ticket (es. T1234), univoco per spazio. */
    @Column(name = "ticket_code")
    private String ticketCode;

    /** Un titolo breve che riassume il problema (es. "Sedia rotta"). */
    @Column(length = 30)
    private String title;

    private Long workerID;
    private Long technicianID;
    private Long deskID;
    /** Descrizione dettagliata del problema. */
    private String description;
    /** Stato corrente ({@link TicketStatus}). */
    private String status = TicketStatus.OPEN.name();
    /** Com'è stato risolto il problema. */
    private String resolution;
    /** Note tecniche del tecnico assegnato. */
    private String technicianNote;
    private LocalDateTime createdAt;

    /** Quando il problema è stato dichiarato risolto. */
    private LocalDateTime resolvedAt;

    /** Severità del ticket ({@link TicketSeverity}). */
    private String severity;

    /** Data/ora stimata di completamento intervento (impostata dal tecnico). */
    private LocalDateTime estimatedResolutionAt;

    /** Collega il ticket al desk e imposta stato OPEN. */
    public void report(Desk desk) {
        this.deskID = desk.getDeskID();
        this.status = TicketStatus.OPEN.name();
        if (desk.getSpace() != null) {
            this.spaceID = desk.getSpace().getSpaceID();
        }
    }

    /** Assegna il tecnico e transiziona a IN_PROGRESS. */
    public void assign(Long technicianID) {
        this.technicianID = technicianID;
        this.status = TicketStatus.IN_PROGRESS.name();
    }

    /** Imposta risoluzione e transiziona a RESOLVED. */
    public void resolve(String resolutionText) {
        this.resolution = resolutionText;
        this.status = TicketStatus.RESOLVED.name();
    }

    /** Forza lo stato a IN_PROGRESS. */
    public void inProgress() {
        this.status = TicketStatus.IN_PROGRESS.name();
    }
}

