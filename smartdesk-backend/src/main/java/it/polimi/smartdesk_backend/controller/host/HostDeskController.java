package it.polimi.smartdesk_backend.controller.host;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.DeskRequestDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.host.HostDeskService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Crea e gestisce le postazioni (anche dismissione desk). */
@RestController
@RequestMapping("/api/hosts/desks")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Postazioni host", description = "Creazione e gestione postazioni lato host.")
@RequiredArgsConstructor
public class HostDeskController {

    private final HostDeskService hostDeskService;
    private final AccessControlService accessControlService;

    /**
     * Crea una nuova postazione nello spazio indicato nel body.
     *
     * @param request dati della postazione (spazio, stanza, amenity)
     * @return postazione creata con HTTP 201
     */
    @PostMapping
    @Operation(summary = "Crea una nuova postazione")
    public ResponseEntity<DeskDTO> createDesk(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DeskRequestDTO request) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(hostDeskService.createDesk(host.getUserId(), request));
    }

    /**
     * Aggiorna i dati di una postazione esistente (nome, stanza, amenity).
     *
     * @param deskID ID del desk da aggiornare
     * @param request nuovi dati
     * @return desk aggiornato
     */
    @PutMapping("/{deskID}")
    @Operation(summary = "Aggiorna i dati di una postazione")
    public ResponseEntity<DeskDTO> updateDesk(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DeskRequestDTO request) {
        var host = me(principal);
        return ResponseEntity.ok(hostDeskService.updateDeskForHost(host.getUserId(), deskID, request));
    }

    /**
     * Approva il ripristino del desk dopo la manutenzione: lo riporta in stato AVAILABLE.
     *
     * @param deskID ID del desk in PENDING_INSPECTION
     */
    @PatchMapping("/{deskID}/inspect")
    @Operation(summary = "Approva il ripristino della postazione dopo manutenzione")
    public ResponseEntity<DeskDTO> approveInspection(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostDeskService.approveInspection(host.getUserId(), deskID));
    }

    /**
     * Dismette definitivamente la postazione: non sarà più prenotabile.
     *
     * @param deskID ID del desk da dismettere
     */
    @PatchMapping("/{deskID}/decommission")
    @Operation(summary = "Dismetti definitivamente la postazione")
    public ResponseEntity<DeskDTO> decommissionDesk(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostDeskService.decommissionDesk(host.getUserId(), deskID));
    }

    /**
     * Rimanda il desk in MAINTENANCE se l'ispezione non è andata a buon fine.
     *
     * @param deskID ID del desk in PENDING_INSPECTION da rimandare indietro
     */
    @PatchMapping("/{deskID}/maintenance")
    @Operation(summary = "Rimette in manutenzione dopo ispezione non superata")
    public ResponseEntity<DeskDTO> rejectInspection(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostDeskService.rejectInspection(host.getUserId(), deskID));
    }

    /**
     * Elimina una postazione dallo spazio.
     *
     * @param deskID ID del desk da rimuovere
     */
    @DeleteMapping("/{deskID}")
    @Operation(summary = "Elimina una postazione dallo spazio")
    public ResponseEntity<Void> removeDesk(
            @PathVariable Long deskID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostDeskService.removeDeskForHost(host.getUserId(), deskID);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

