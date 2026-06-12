package it.polimi.smartdesk_backend.controller.space;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceOperations;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import lombok.RequiredArgsConstructor;

/** Spazi {@code approved=true} per worker/SYS_ADMIN: elenco e dettaglio pre-prenotazione. */
@RestController
@RequestMapping("/api/spaces")
@PreAuthorize("hasAnyRole('WORKER','SYS_ADMIN')")
@Tag(name = "Spazi", description = "Solo spazi approvati: elenco e dettaglio per esplorazione pre-prenotazione.")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceOperations spaceOperations;
    private final AccessControlService accessControlService;

    /** Homepage con l'elenco di tutti gli uffici approvati. */
    @GetMapping
    @Operation(summary = "Elenca tutti gli spazi approvati")
    public ResponseEntity<List<SpaceDTO>> listSpaces(@AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(spaceOperations.findAll());
    }

    /** Restituisce il dettaglio di uno spazio approvato; 404 se non approvato o inesistente. */
    @GetMapping("/{spaceId}")
    @Operation(summary = "Dettaglio spazio approvato per ID")
    public ResponseEntity<SpaceDTO> getSpace(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(spaceOperations.findById(spaceId));
    }
}


