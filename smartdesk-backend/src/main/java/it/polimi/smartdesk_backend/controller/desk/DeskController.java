package it.polimi.smartdesk_backend.controller.desk;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.desk.DeskService;

/** Catalogo desk: elenco filtrato per spazio e disponibilità giornaliera (spazi approvati). */
@RestController
@RequestMapping("/api/desks")
@PreAuthorize("hasAnyRole('WORKER','HOST','SYS_ADMIN')")
@Tag(name = "Postazioni", description = "Catalogo postazioni, filtri per spazio e disponibilità per giorno.")
@RequiredArgsConstructor
public class DeskController {

    private final DeskService deskService;
    private final AccessControlService accessControlService;

    /** Restituisce l'elenco delle postazioni; con {@code spaceId} filtra per ufficio approvato. */
    @GetMapping
    @Operation(
            summary = "Elenca postazioni, opzionalmente filtrate per spazio",
            description = "Se spaceId è valorizzato, le postazioni vengono restituite solo se lo spazio esiste ed è approvato; "
                    + "altrimenti l'API risponde 404.")
    public ResponseEntity<List<DeskDTO>> listDesks(
            @RequestParam(required = false) Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        if (spaceId != null) {
            if (requester.getRole() == Role.HOST) {
                return ResponseEntity.ok(deskService.findBySpaceForHost(requester.getUserId(), spaceId));
            }
            return ResponseEntity.ok(deskService.findBySpace(spaceId));
        }
        return ResponseEntity.ok(deskService.findAllApproved());
    }

    /** Restituisce il dettaglio di una postazione approvata. */
    @GetMapping("/{deskId}")
    @Operation(summary = "Dettaglio postazione per ID")
    public ResponseEntity<DeskDTO> getDesk(
            @PathVariable Long deskId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(deskService.findApprovedById(deskId));
    }

    /** Desk con almeno uno slot prenotabile nella {@code date} (stato + assenza booking sovrapposti). */
    @GetMapping("/available")
    @Operation(summary = "Elenca postazioni disponibili in una data")
    public ResponseEntity<List<DeskDTO>> getAvailableDesks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(deskService.findAvailable(date));
    }
}


