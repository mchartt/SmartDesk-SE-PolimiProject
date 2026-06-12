package it.polimi.smartdesk_backend.controller.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.booking.WaitlistStatusDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.BookingWaitlistService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import lombok.RequiredArgsConstructor;

/** Lista d'attesa: il worker viene notificato quando si libera uno slot sul desk nella data indicata. */
@RestController
@RequestMapping("/api/workers/desks/{deskID}/waitlist")
@PreAuthorize("hasRole('WORKER')")
@Tag(name = "Lista d'attesa worker", description = "Iscrizione alla lista d'attesa per postazioni occupate.")
@RequiredArgsConstructor
public class WorkerWaitlistController {

    private final BookingWaitlistService bookingWaitlistService;
    private final AccessControlService accessControlService;

    /**
     * Iscrive il worker alla lista d'attesa per un desk in una data specifica.
     * Se il desk si libera nello slot indicato, il worker riceve una notifica.
     *
     * @param deskID ID del desk
     * @param date giorno per cui il worker richiede notifica di disponibilità
     * @param desiredStart orario di inizio desiderato (opzionale)
     * @param desiredEnd orario di fine desiderato (opzionale)
     */
    @PostMapping
    @Operation(summary = "Iscrive il worker alla lista d'attesa")
    public ResponseEntity<Void> joinWaitlist(
            @PathVariable Long deskID,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desiredStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desiredEnd,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        bookingWaitlistService.notifyMeWhenAvailable(deskID, date, worker.getUserId(), desiredStart, desiredEnd);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifica se il worker è in lista d'attesa per un desk in una certa data.
     *
     * @param deskID ID del desk
     * @param date giorno da verificare
     * @return stato attuale nella waitlist (posizione, se in coda o no)
     */
    @GetMapping
    @Operation(summary = "Verifica lo stato in lista d'attesa")
    public ResponseEntity<WaitlistStatusDTO> getWaitlistStatus(
            @PathVariable Long deskID,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(bookingWaitlistService.getWaitlistStatus(deskID, date, worker.getUserId()));
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

