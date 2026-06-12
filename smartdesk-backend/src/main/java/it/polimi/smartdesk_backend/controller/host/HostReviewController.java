package it.polimi.smartdesk_backend.controller.host;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.review.HostReviewService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Recensioni ricevute: l'host consulta le recensioni e le segna come lette. */
@RestController
@RequestMapping("/api/hosts")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Recensioni host", description = "Lettura, risposta e gestione recensioni ricevute.")
@RequiredArgsConstructor
public class HostReviewController {

    private final HostReviewService hostReviewService;
    private final SpaceManagementService spaceManagementService;
    private final AccessControlService accessControlService;

    /**
     * Ritorna tutte le recensioni ricevute dall'host, con eventuali risposte già date.
     *
     * @param hostID ID dell'host (deve coincidere con il principal)
     */
    @GetMapping("/{hostID}/reviews")
    @Operation(summary = "Elenca le recensioni ricevute dall'host")
    public ResponseEntity<List<ReviewResponseDTO>> getMyReviews(
            @PathVariable Long hostID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertHostOwnsPath(principal, hostID, "reviews");
        return ResponseEntity.ok(hostReviewService.getReviewResponsesForHost(hostID));
    }

    /**
     * Ritorna le recensioni ricevute su uno spazio specifico dell'host.
     *
     * @param spaceID ID dello spazio (deve appartenere all'host autenticato)
     */
    @GetMapping("/spaces/{spaceID}/reviews")
    @Operation(summary = "Elenca le recensioni di uno spazio")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForSpace(
            @PathVariable Long spaceID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        spaceManagementService.assertHostOwnsSpace(host.getUserId(), spaceID);
        return ResponseEntity.ok(hostReviewService.getReviewResponsesForSpace(spaceID));
    }

    /**
     * L'host segna una recensione come vista, senza rispondere.
     *
     * @param spaceID ID dello spazio
     * @param reviewID ID della recensione da marcare come letta
     * @return recensione aggiornata
     */
    @PatchMapping("/spaces/{spaceID}/reviews/{reviewID}/seen")
    @Operation(summary = "Segna una recensione come letta")
    public ResponseEntity<ReviewResponseDTO> markReviewSeenByHost(
            @PathVariable Long spaceID,
            @PathVariable Long reviewID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostReviewService.markReviewSeenByHostResponse(host.getUserId(), spaceID, reviewID));
    }


    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

