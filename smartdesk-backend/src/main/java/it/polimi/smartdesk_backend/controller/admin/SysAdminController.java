package it.polimi.smartdesk_backend.controller.admin;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.UserModerationAction;
import it.polimi.smartdesk_backend.service.booking.BookingCancellationService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.review.AdminReviewService;
import it.polimi.smartdesk_backend.service.admin.AdminSpaceOperations;
import it.polimi.smartdesk_backend.service.admin.SysAdminService;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.util.support.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Operazioni privilegiate per amministratori: moderazione utenti, approvazione sedi, gestione forzata prenotazioni e audit log. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Amministrazione", description = "Moderazione utenti, approvazione spazi, prenotazioni e log di sistema.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class SysAdminController {

    private final SysAdminService sysAdminService;
    private final AdminSpaceOperations adminSpaceOperations;
    private final AdminReviewService adminReviewService;
    private final BookingCancellationService bookingCancellationService;
    private final BookingQueryService bookingQueryService;

    // --- Moderazione Utenti ---

    /**
     * Applica un'azione di moderazione su un utente (es. ban, unban).
     *
     * @param userID ID dell'utente target
     * @param action azione da eseguire
     * @param principal admin autenticato che esegue l'operazione
     * @param httpRequest usato per estrarre l'IP per l'audit log
     */
    @PatchMapping("/users/{userID}")
    @Operation(summary = "Applica un'azione di moderazione su un utente")
    public ResponseEntity<Void> moderateUser(
            @PathVariable Long userID,
            @RequestParam UserModerationAction action,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest) {
        Long adminId = principal != null ? principal.getUserId() : null;
        sysAdminService.moderateUser(adminId, userID, action, RequestUtils.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    /** Ritorna tutti gli utenti registrati nel sistema. */
    @GetMapping("/users")
    @Operation(summary = "Elenca tutti gli utenti registrati nel sistema")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(sysAdminService.getAllUsers());
    }

    /** Lista i host che hanno richiesto registrazione ma non sono ancora stati approvati. */
    @GetMapping("/hosts/pending")
    @Operation(summary = "Elenca gli host in attesa di approvazione")
    public ResponseEntity<List<UserProfileDTO>> getPendingHosts() {
        return ResponseEntity.ok(sysAdminService.getPendingHosts());
    }

    /**
     * Approva la registrazione di un host: da quel momento può creare spazi.
     *
     * @param hostID ID dell'host da approvare
     * @param request usato per ricavare l'IP per l'audit
     */
    @PatchMapping("/hosts/{hostID}/approve")
    @Operation(summary = "Approva la registrazione di un host")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveHost(@PathVariable Long hostID, HttpServletRequest request) {
        sysAdminService.approveHost(hostID, RequestUtils.getClientIp(request));
    }

    /**
     * Respinge la registrazione di un host.
     *
     * @param hostID ID dell'host da rifiutare
     * @param request usato per ricavare l'IP per l'audit
     */
    @PatchMapping("/hosts/{hostID}/reject")
    @Operation(summary = "Respinge la registrazione di un host")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectHost(@PathVariable Long hostID, HttpServletRequest request) {
        sysAdminService.rejectHost(hostID, RequestUtils.getClientIp(request));
    }

    // --- Gestione Spazi ---

    /** Lista tutti gli spazi presenti, approvati o no. */
    @GetMapping("/spaces")
    @Operation(summary = "Elenca tutti gli spazi del sistema")
    public ResponseEntity<List<SpaceDTO>> getAllSpaces() {
        return ResponseEntity.ok(adminSpaceOperations.findAllForAdmin());
    }

    /** Lista gli spazi già approvati, con dati arricchiti (rating, host). */
    @GetMapping("/spaces/approved")
    @Operation(summary = "Elenca gli spazi approvati con dati arricchiti")
    public ResponseEntity<List<SpaceDTO>> getApprovedSpaces() {
        return ResponseEntity.ok(adminSpaceOperations.findApprovedEnrichedForAdmin());
    }

    /** Lista gli spazi in attesa di approvazione. */
    @GetMapping("/spaces/pending")
    @Operation(summary = "Elenca gli spazi in attesa di approvazione")
    public ResponseEntity<List<SpaceDTO>> getPendingSpaces() {
        return ResponseEntity.ok(adminSpaceOperations.findPendingApprovalForAdmin());
    }

    /**
     * Approva uno spazio: da quel momento è visibile ai worker.
     *
     * @param spaceId ID dello spazio
     * @param request per ricavare l'IP per l'audit
     */
    @PatchMapping("/spaces/{spaceId}/approve")
    @Operation(summary = "Approva uno spazio coworking")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveSpace(@PathVariable Long spaceId, HttpServletRequest request) {
        sysAdminService.approveSpace(spaceId, RequestUtils.getClientIp(request));
    }

    /**
     * Respinge uno spazio: non sarà visibile nel catalogo.
     *
     * @param spaceId ID dello spazio
     * @param request per ricavare l'IP per l'audit
     */
    @PatchMapping("/spaces/{spaceId}/reject")
    @Operation(summary = "Respinge uno spazio coworking")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectSpace(@PathVariable Long spaceId, HttpServletRequest request) {
        sysAdminService.rejectSpace(spaceId, RequestUtils.getClientIp(request));
    }

    /**
     * Forza la chiusura di uno spazio approvato (es. in caso di violazioni gravi).
     *
     * @param spaceId ID dello spazio da chiudere
     * @param request per ricavare l'IP per l'audit
     */
    @DeleteMapping("/spaces/{spaceId}")
    @Operation(summary = "Forza la chiusura di uno spazio approvato")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceCloseSpace(@PathVariable Long spaceId, HttpServletRequest request) {
        sysAdminService.forceCloseSpace(spaceId, RequestUtils.getClientIp(request));
    }

    // --- Gestione Recensioni ---

    /**
     * Ritorna tutte le recensioni di uno spazio, incluse le risposte dell'host.
     *
     * @param spaceID ID dello spazio
     */
    @GetMapping("/spaces/{spaceID}/reviews")
    @Operation(summary = "Elenca le recensioni di uno spazio")
    public ResponseEntity<List<ReviewResponseDTO>> getSpaceReviews(@PathVariable Long spaceID) {
        return ResponseEntity.ok(adminReviewService.getReviewResponsesForSpaceForAdmin(spaceID));
    }

    /**
     * Elimina una recensione: usato per rimuovere contenuti inappropriati.
     *
     * @param reviewID ID della recensione da eliminare
     */
    @DeleteMapping("/reviews/{reviewID}")
    @Operation(summary = "Elimina una recensione inappropriata")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewID) {
        adminReviewService.deleteReviewAsAdmin(reviewID);
        return ResponseEntity.noContent().build();
    }

    // --- Gestione Prenotazioni ---

    /** Ritorna tutte le prenotazioni nel sistema, senza filtri. */
    @GetMapping("/bookings")
    @Operation(summary = "Elenca tutte le prenotazioni del sistema")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingQueryService.getAllBookingsForAdmin());
    }

    /**
     * Cancella forzatamente una prenotazione a nome dell'admin.
     *
     * @param bookingId ID della prenotazione da cancellare
     * @param principal admin che esegue l'operazione
     */
    @DeleteMapping("/bookings/{bookingId}")
    @Operation(summary = "Cancella forzatamente una prenotazione")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Long adminId = principal != null ? principal.getUserId() : null;
        bookingCancellationService.removeBookingForUser(bookingId, adminId, Role.SYS_ADMIN);
        return ResponseEntity.noContent().build();
    }

    // --- Audit e Log ---

    /** Ritorna il log di sistema con tutte le azioni registrate dagli admin. */
    @GetMapping("/logs")
    @Operation(summary = "Elenca il log di audit di sistema")
    public ResponseEntity<List<LogDTO>> getSystemLogs() {
        return ResponseEntity.ok(sysAdminService.getSystemLogs());
    }
}
