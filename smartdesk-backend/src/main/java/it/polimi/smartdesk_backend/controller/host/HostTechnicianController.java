package it.polimi.smartdesk_backend.controller.host;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.HostTechnicianCreateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.HostTechnicianUpdateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.host.HostTechnicianAccountService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianDashboardService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** L'host crea i tecnici, li assegna agli spazi e consulta la dashboard dei tecnici. */
@RestController
@RequestMapping("/api/hosts")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Tecnici host", description = "Gestione account tecnici e assegnazioni agli spazi.")
@RequiredArgsConstructor
public class HostTechnicianController {

    private final HostTechnicianAccountService hostTechnicianAccountService;
    private final HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;
    private final HostTechnicianDashboardService hostTechnicianDashboardService;
    private final AccessControlService accessControlService;

    /**
     * Crea un account tecnico associato a questo host.
     *
     * @param request credenziali e dati del tecnico
     * @return tecnico creato con HTTP 201
     */
    @PostMapping("/technicians")
    @Operation(summary = "Crea un account tecnico")
    public ResponseEntity<TechnicianDTO> createTechnician(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody HostTechnicianCreateRequestDTO request) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(hostTechnicianAccountService.createTechnician(host.getUserId(), request));
    }

    /**
     * Assegna un tecnico a uno spazio: da quel momento potrà gestirne i ticket.
     *
     * @param spaceId ID dello spazio
     * @param technicianId ID del tecnico da assegnare
     */
    @PostMapping("/spaces/{spaceId}/technicians/{technicianId}")
    @Operation(summary = "Assegna un tecnico a uno spazio")
    public ResponseEntity<TechnicianDTO> assignTechnicianToSpace(
            @PathVariable Long spaceId,
            @PathVariable Long technicianId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostTechnicianSpaceManagementService.assignTechnicianToSpace(host.getUserId(), spaceId, technicianId));
    }

    /**
     * Rimuove l'assegnazione di un tecnico da uno spazio.
     *
     * @param spaceId ID dello spazio
     * @param technicianId ID del tecnico da de-assegnare
     */
    @DeleteMapping("/spaces/{spaceId}/technicians/{technicianId}")
    @Operation(summary = "Rimuove un tecnico da uno spazio")
    public ResponseEntity<Void> unassignTechnicianFromSpace(
            @PathVariable Long spaceId,
            @PathVariable Long technicianId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostTechnicianSpaceManagementService.unassignTechnicianFromSpace(host.getUserId(), spaceId, technicianId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elenca i tecnici assegnati a uno spazio.
     *
     * @param spaceId ID dello spazio
     */
    @GetMapping("/spaces/{spaceId}/technicians")
    @Operation(summary = "Elenca i tecnici assegnati a uno spazio")
    public ResponseEntity<List<TechnicianDTO>> getTechniciansForSpace(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostTechnicianSpaceManagementService.getTechniciansForSpace(host.getUserId(), spaceId));
    }

    /** Lista tutti i tecnici creati dall'host, con un riepilogo dei ticket aperti per ognuno. */
    @GetMapping("/technicians")
    @Operation(summary = "Elenca i tecnici dell'host con riepilogo ticket")
    public ResponseEntity<List<TechnicianDTO>> getTechniciansForHostDashboard(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostTechnicianDashboardService.getTechniciansForHost(host.getUserId()));
    }

    /**
     * Aggiorna i dati di un tecnico (nome, email, password).
     *
     * @param technicianId ID del tecnico da aggiornare
     * @param body nuovi dati
     */
    @PutMapping("/technicians/{technicianId}")
    @Operation(summary = "Aggiorna i dati di un tecnico")
    public ResponseEntity<TechnicianDTO> updateTechnician(
            @PathVariable Long technicianId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody HostTechnicianUpdateRequestDTO body) {
        var host = me(principal);
        return ResponseEntity.ok(hostTechnicianAccountService.updateTechnicianForHost(host.getUserId(), technicianId, body));
    }

    /**
     * Elimina l'account di un tecnico.
     *
     * @param technicianId ID del tecnico da eliminare
     */
    @DeleteMapping("/technicians/{technicianId}")
    @Operation(summary = "Elimina l'account di un tecnico")
    public ResponseEntity<Void> deleteTechnician(
            @PathVariable Long technicianId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostTechnicianAccountService.deleteTechnicianForHost(host.getUserId(), technicianId);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

