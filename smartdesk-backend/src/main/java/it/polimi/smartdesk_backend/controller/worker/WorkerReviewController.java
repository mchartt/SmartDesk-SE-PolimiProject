package it.polimi.smartdesk_backend.controller.worker;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import jakarta.validation.Valid;
import it.polimi.smartdesk_backend.dto.review.ReviewDTO;
import it.polimi.smartdesk_backend.dto.review.WorkerReviewHistoryDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.review.WorkerReviewService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import lombok.RequiredArgsConstructor;

/** Dopo la giornata in ufficio: il worker invia recensioni sugli spazi visitati e gestisce le proprie recensioni. */
@RestController
@RequestMapping("/api/workers/reviews")
@PreAuthorize("hasRole('WORKER')")
@Tag(name = "Recensioni worker", description = "Invio e gestione recensioni dopo le prenotazioni.")
@RequiredArgsConstructor
public class WorkerReviewController {

    private final WorkerReviewService workerReviewService;
    private final AccessControlService accessControlService;

    /** Ritorna lo storico di tutte le recensioni lasciate dal worker. */
    @GetMapping("/history")
    @Operation(summary = "Elenca lo storico recensioni del worker")
    public ResponseEntity<List<WorkerReviewHistoryDTO>> getMyReviews(@AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(workerReviewService.getWorkerReviewHistory(worker.getUserId()));
    }

    /**
     * Il worker lascia una recensione su uno spazio già visitato.
     *
     * @param review voto e testo della recensione
     * @return recensione creata con HTTP 201
     */
    @PostMapping
    @Operation(summary = "Invia una recensione su uno spazio visitato")
    public ResponseEntity<WorkerReviewHistoryDTO> leaveReview(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ReviewDTO review) {
        var worker = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerReviewService.leaveReviewForWorker(worker.getUserId(), review));
    }

    /**
     * Modifica una recensione esistente (voto o testo).
     *
     * @param reviewID ID della recensione da aggiornare
     * @param review nuovi dati
     * @return recensione aggiornata
     */
    @PatchMapping("/{reviewID}")
    @Operation(summary = "Aggiorna una recensione esistente")
    public ResponseEntity<WorkerReviewHistoryDTO> updateReview(
            @PathVariable Long reviewID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ReviewDTO review) {
        var worker = me(principal);
        return ResponseEntity.ok(workerReviewService.updateReviewForWorker(worker.getUserId(), reviewID, review));
    }

    /**
     * Elimina una recensione lasciata dal worker.
     *
     * @param reviewID ID della recensione da eliminare
     */
    @DeleteMapping("/{reviewID}")
    @Operation(summary = "Elimina una recensione del worker")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        workerReviewService.deleteReviewAsWorker(worker.getUserId(), reviewID);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

