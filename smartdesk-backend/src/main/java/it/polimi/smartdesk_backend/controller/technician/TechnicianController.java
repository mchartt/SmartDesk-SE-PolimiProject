package it.polimi.smartdesk_backend.controller.technician;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketCommentRequestDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.dto.common.TicketStatusRequest;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TechnicianAssignmentService;
import it.polimi.smartdesk_backend.service.desk.TechnicianDeskMaintenanceService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import jakarta.validation.Valid;

/** Operazioni del tecnico: spazi assegnati, ticket, commenti e gestione manutenzione postazioni. */
@RestController
@RequestMapping("/api/technicians")
@PreAuthorize("hasRole('TECHNICIAN')")
@Tag(name = "Tecnici", description = "Ticket, assegnazioni spazio/postazione e modalità manutenzione per tecnici.")
@RequiredArgsConstructor
public class TechnicianController {

    private final TicketService ticketService;
    private final TicketCommentService ticketCommentService;
    private final TechnicianAssignmentService technicianAssignmentService;
    private final TechnicianDeskMaintenanceService technicianDeskMaintenanceService;
    private final AccessControlService accessControlService;

    /** Elenca gli spazi assegnati al tecnico autenticato. */
    @GetMapping("/spaces")
    @Operation(summary = "Elenca gli spazi coworking assegnati al tecnico")
    public ResponseEntity<List<TechnicianAssignedSpaceDTO>> getMySpaces(@AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(technicianAssignmentService.listAssignedSpaces(technician.getUserId()));
    }

    /** Elenca le postazioni di uno spazio assegnato al tecnico autenticato. */
    @GetMapping("/spaces/{spaceID}/desks")
    @Operation(summary = "Elenca le postazioni in uno spazio assegnato")
    public ResponseEntity<List<DeskDTO>> getDesksInAssignedSpace(
            @PathVariable Long spaceID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(technicianAssignmentService.listAssignedDesks(technician.getUserId(), spaceID));
    }

    /** Elenca i ticket ancora in attesa di presa in carico negli spazi assegnati al tecnico. */
    @GetMapping("/tickets/pending")
    @Operation(summary = "Elenca i ticket in attesa per i tecnici")
    public ResponseEntity<List<TicketResponseDTO>> getPendingTickets(@AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(ticketService.getPendingTicketResponses(technician.getUserId()));
    }

    /** Elenca i ticket assegnati al tecnico autenticato. */
    @GetMapping("/tickets/assigned")
    @Operation(summary = "Elenca i ticket assegnati al tecnico corrente")
    public ResponseEntity<List<TicketResponseDTO>> getAssignedTickets(@AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(ticketService.getTicketResponsesByTechnician(technician.getUserId()));
    }

    /** Elimina dall'archivio i ticket RESOLVED/CLOSED assegnati a questo tecnico. */
    @DeleteMapping("/tickets/resolved-history")
    @Operation(summary = "Svuota lo storico ticket risolti del tecnico")
    public ResponseEntity<Map<String, Integer>> clearResolvedTicketHistory(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        int deleted = ticketService.clearResolvedTicketHistoryForTechnician(technician.getUserId());
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    /** Restituisce il dettaglio di un ticket assegnato al tecnico autenticato. */
    @GetMapping("/tickets/{ticketID}")
    @Operation(summary = "Dettaglio ticket assegnato per ID")
    public ResponseEntity<TicketResponseDTO> getAssignedTicketById(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(ticketService.getTicketResponseByIdForRequester(
                ticketID, technician.getUserId(), Role.TECHNICIAN));
    }

    /** Aggiunge un commento al thread del ticket (solo se assegnato e non chiuso). */
    @PostMapping("/tickets/{ticketID}/comments")
    @Operation(summary = "Aggiunge un commento a un ticket assegnato")
    public ResponseEntity<TicketResponseDTO> addTicketComment(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TicketCommentRequestDTO request) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(
                ticketCommentService.addComment(
                        technician.getUserId(), ticketID, Role.TECHNICIAN, request.getBody()));
    }

    /**
     * Aggiorna lo stato di un ticket (es. IN_PROGRESS o VERIFYING).
     * Se il ticket non aveva tecnico assegnato, l'operazione assegna automaticamente il tecnico autenticato.
     *
     * @param ticketID ID del ticket
     * @param request nuovo stato, nota, testo risoluzione, gravità e data stimata
     * @return ticket aggiornato
     */
    @PatchMapping("/tickets/{ticketID}")
    @Operation(summary = "Aggiorna stato, nota e risoluzione di un ticket assegnato")
    public ResponseEntity<TicketResponseDTO> updateTicketStatus(
            @PathVariable Long ticketID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TicketStatusRequest request) {
        var technician = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(ticketService.updateTicketStatusForTechnician(
                technician.getUserId(),
                ticketID,
                request.status(),
                request.note(),
                request.resolution(),
                request.severity(),
                request.estimatedResolutionAt()));
    }

    /** Porta una postazione in manutenzione durante l'intervento del tecnico; le nuove prenotazioni sono bloccate. */
    @PatchMapping("/desks/{deskID}/maintenance")
    @Operation(summary = "Mette una postazione in manutenzione")
    public ResponseEntity<Void> setMaintenanceMode(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        technicianDeskMaintenanceService.setDeskMaintenanceForTechnician(technician.getUserId(), deskID);
        return ResponseEntity.noContent().build();
    }

    /** Termina la manutenzione: la postazione passa in attesa di ispezione host. */
    @org.springframework.web.bind.annotation.DeleteMapping("/desks/{deskID}/maintenance")
    @Operation(summary = "Rimuove una postazione dalla manutenzione")
    public ResponseEntity<Void> removeMaintenanceMode(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var technician = accessControlService.assertAuthenticated(principal);
        technicianDeskMaintenanceService.revertDeskMaintenanceForTechnician(technician.getUserId(), deskID);
        return ResponseEntity.noContent().build();
    }
}


