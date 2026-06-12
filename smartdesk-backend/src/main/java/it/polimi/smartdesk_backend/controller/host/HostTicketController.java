package it.polimi.smartdesk_backend.controller.host;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import lombok.RequiredArgsConstructor;

/** Segnalazioni sui desk dell'host — assegna tecnico, approva o respingi, storico risolti. */
@RestController
@RequestMapping("/api/hosts")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Ticket host", description = "Assegnazione tecnici, approvazione e gestione segnalazioni.")
@RequiredArgsConstructor
public class HostTicketController {

    private final TicketService ticketService;
    private final TicketCommentService ticketCommentService;
    private final HostOwnershipService hostOwnershipService;
    private final AccessControlService accessControlService;

    /**
     * Elenca tutti i ticket aperti su un desk dell'host (solo desk di proprietà).
     *
     * @param deskId ID del desk
     */
    @GetMapping("/desks/{deskId}/tickets")
    @Operation(summary = "Elenca i ticket aperti su una postazione")
    public ResponseEntity<List<TicketResponseDTO>> getDeskTickets(
            @PathVariable Long deskId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(host.getUserId(), deskId);
        return ResponseEntity.ok(ticketService.getTicketsByDesk(deskId));
    }

    /**
     * Assegna un tecnico a un ticket e opzionalmente imposta la gravità.
     *
     * @param ticketId ID del ticket
     * @param technicianId ID del tecnico da assegnare
     * @param request body opzionale con il campo {@code severity}
     */
    @PostMapping("/tickets/{ticketId}/technicians/{technicianId}")
    @Operation(summary = "Assegna un tecnico a un ticket")
    public ResponseEntity<TicketResponseDTO> assignTechnicianToTicket(
            @PathVariable Long ticketId,
            @PathVariable Long technicianId,
            @RequestBody(required = false) Map<String, String> request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        String severity = request == null ? null : request.get("severity");
        return ResponseEntity.ok(ticketService.assignTechnicianToTicket(host.getUserId(), ticketId, technicianId, severity));
    }

    /**
     * Elenca i ticket risolti/chiusi sugli spazi dell'host per la dashboard storica.
     *
     * @param limit numero massimo di ticket da restituire (default 80)
     */
    @GetMapping("/resolved-tickets")
    @Operation(summary = "Elenca i ticket risolti dell'host")
    public ResponseEntity<List<TicketResponseDTO>> getResolvedTicketsForHostDashboard(
            @RequestParam(name = "limit", defaultValue = "80") int limit,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(ticketService.getResolvedTicketsForHost(host.getUserId(), limit));
    }

    /**
     * Svuota l'archivio dei ticket risolti dell'host.
     *
     * @return mappa con il numero di record eliminati
     */
    @DeleteMapping("/resolved-tickets")
    @Operation(summary = "Svuota lo storico ticket risolti dell'host")
    public ResponseEntity<Map<String, Integer>> clearResolvedTicketsForHost(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        int deleted = ticketService.clearResolvedTicketHistoryForHost(host.getUserId());
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    /**
     * Approva la risoluzione proposta dal tecnico: il ticket passa in stato RESOLVED.
     *
     * @param ticketId ID del ticket in attesa di approvazione
     */
    @PostMapping("/tickets/{ticketId}/approve")
    @Operation(summary = "Approva la risoluzione di un ticket")
    public ResponseEntity<TicketResponseDTO> approveTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(ticketService.hostApproveTicket(host.getUserId(), ticketId));
    }

    /**
     * Respinge la risoluzione del tecnico e opzionalmente riassegna a un altro tecnico.
     *
     * @param ticketId ID del ticket
     * @param request body opzionale con {@code newTechnicianId} e {@code reason}
     */
    @PostMapping("/tickets/{ticketId}/reject")
    @Operation(summary = "Respinge la risoluzione di un ticket")
    public ResponseEntity<TicketResponseDTO> rejectTicket(
            @PathVariable Long ticketId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        Long newTechId = request != null && request.get("newTechnicianId") != null ? Long.valueOf(request.get("newTechnicianId").toString()) : null;
        String reason = request != null && request.get("reason") != null ? request.get("reason").toString() : null;
        return ResponseEntity.ok(ticketService.hostRejectTicket(host.getUserId(), ticketId, newTechId, reason));
    }

    /**
     * Aggiunge un commento dell'host al thread del ticket.
     *
     * @param ticketId ID del ticket
     * @param request body con il campo {@code body} (testo del commento)
     */
    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Aggiunge un commento al ticket")
    public ResponseEntity<TicketResponseDTO> addHostComment(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        String body = request.get("body");
        return ResponseEntity.ok(ticketCommentService.addComment(host.getUserId(), ticketId, Role.HOST, body));
    }

    /**
     * Chiude il ticket dismettendo il desk: il ticket passa in CLOSED e il desk in DECOMMISSIONED.
     *
     * @param ticketId ID del ticket da chiudere con dismissione desk
     */
    @PostMapping("/tickets/{ticketId}/dismiss-desk")
    @Operation(summary = "Chiude un ticket dismettendo la postazione")
    public ResponseEntity<TicketResponseDTO> dismissDesk(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(ticketService.hostDismissDesk(host.getUserId(), ticketId));
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

