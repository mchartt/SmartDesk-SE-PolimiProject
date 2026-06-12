package it.polimi.smartdesk_backend.controller.worker;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.ticket.TicketCommentRequestDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Il worker apre segnalazioni sul desk prenotato, scambia commenti col tecnico e può cancellare ticket non ancora presi in carico. */
@RestController
@RequestMapping("/api/workers/tickets")
@PreAuthorize("hasRole('WORKER')")
@Tag(name = "Ticket worker", description = "Apertura e gestione segnalazioni sui desk prenotati.")
@RequiredArgsConstructor
public class WorkerTicketController {

    private final TicketService ticketService;
    private final TicketCommentService ticketCommentService;
    private final AccessControlService accessControlService;

    /**
     * Il worker apre un nuovo ticket di segnalazione su un desk.
     *
     * @param ticket dati del problema (desk, descrizione, gravità)
     * @return HTTP 201 senza corpo
     */
    @PostMapping
    @Operation(summary = "Apre una segnalazione su una postazione")
    public ResponseEntity<Void> reportIssue(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TicketDTO ticket) {
        var worker = me(principal);
        ticketService.openTicket(worker.getUserId(), ticket);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Ritorna tutti i ticket aperti o già chiusi da questo worker. */
    @GetMapping
    @Operation(summary = "Elenca i ticket del worker")
    public ResponseEntity<List<TicketResponseDTO>> getMyTickets(@AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(ticketService.getTicketResponsesByWorker(worker.getUserId()));
    }

    /**
     * Ritorna il dettaglio di un singolo ticket del worker.
     *
     * @param ticketID ID del ticket
     */
    @GetMapping("/{ticketID}")
    @Operation(summary = "Dettaglio di un ticket del worker")
    public ResponseEntity<TicketResponseDTO> getMyTicketById(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(ticketService.getTicketResponseByIdForRequester(ticketID, worker.getUserId(), Role.WORKER));
    }

    /**
     * Aggiunge un commento al thread del ticket (solo se non ancora chiuso).
     *
     * @param ticketID ID del ticket
     * @param request testo del commento
     * @return ticket aggiornato con il nuovo commento
     */
    @PostMapping("/{ticketID}/comments")
    @Operation(summary = "Aggiunge un commento a un ticket")
    public ResponseEntity<TicketResponseDTO> addTicketComment(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TicketCommentRequestDTO request) {
        var worker = me(principal);
        return ResponseEntity.ok(
                ticketCommentService.addComment(worker.getUserId(), ticketID, Role.WORKER, request.getBody()));
    }

    /**
     * Elimina un ticket aperto dal worker (solo se non ancora preso in carico dal tecnico).
     *
     * @param ticketID ID del ticket da eliminare
     */
    @DeleteMapping("/{ticketID}")
    @Operation(summary = "Elimina un ticket non ancora preso in carico")
    public ResponseEntity<Void> deleteMyTicket(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        ticketService.deleteTicketForRequester(ticketID, worker.getUserId(), Role.WORKER);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

