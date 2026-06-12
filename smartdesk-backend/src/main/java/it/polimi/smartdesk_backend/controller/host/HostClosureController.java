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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.SpaceClosureCreateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceClosureDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.space.SpaceClosureService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Marca i giorni in cui l'ufficio resta chiuso (niente prenotazioni). */
@RestController
@RequestMapping("/api/hosts/spaces/{spaceId}/closures")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Chiusure host", description = "Giorni di chiusura straordinaria degli spazi.")
@RequiredArgsConstructor
public class HostClosureController {

    private final SpaceClosureService spaceClosureService;
    private final AccessControlService accessControlService;

    /**
     * Elenca tutte le chiusure programmate per uno spazio dell'host, ordinate per data.
     *
     * @param spaceId ID dello spazio
     */
    @GetMapping
    @Operation(summary = "Elenca le chiusure programmate di uno spazio")
    public ResponseEntity<List<SpaceClosureDTO>> listSpaceClosures(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(spaceClosureService.listForHost(host.getUserId(), spaceId));
    }

    /**
     * Crea una o più chiusure per lo spazio.
     * Le prenotazioni attive nei giorni indicati vengono cancellate e i worker notificati.
     *
     * @param spaceId ID dello spazio
     * @param body elenco di date e motivazione della chiusura
     * @return lista delle chiusure create con HTTP 201
     */
    @PostMapping
    @Operation(summary = "Crea chiusure straordinarie per uno spazio")
    public ResponseEntity<List<SpaceClosureDTO>> createSpaceClosures(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SpaceClosureCreateRequestDTO body) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceClosureService.createClosuresForHost(host.getUserId(), spaceId, body));
    }

    /**
     * Elimina una chiusura: lo spazio torna aperto in quel giorno.
     * Le prenotazioni precedentemente cancellate non vengono ripristinate.
     *
     * @param spaceId ID dello spazio
     * @param closureId ID della chiusura da rimuovere
     */
    @DeleteMapping("/{closureId}")
    @Operation(summary = "Elimina una chiusura programmata")
    public ResponseEntity<Void> deleteSpaceClosure(
            @PathVariable Long spaceId,
            @PathVariable Long closureId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        spaceClosureService.deleteClosureForHost(host.getUserId(), spaceId, closureId);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

