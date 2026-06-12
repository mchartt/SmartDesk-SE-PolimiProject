package it.polimi.smartdesk_backend.controller.worker;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.CacheControl;
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
import it.polimi.smartdesk_backend.dto.booking.SlotStatusDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceClosureDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.DeskAvailabilityService;
import it.polimi.smartdesk_backend.service.space.SpaceClosureService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import lombok.RequiredArgsConstructor;

/** Sfoglia uffici approvati, chiusure e slot prima di prenotare. */
@RestController
@RequestMapping("/api/workers")
@PreAuthorize("hasRole('WORKER')")
@Tag(name = "Spazi worker", description = "Esplorazione spazi approvati, chiusure e disponibilità.")
@RequiredArgsConstructor
public class WorkerSpaceController {

    private final SpaceManagementService spaceManagementService;
    private final SpaceClosureService spaceClosureService;
    private final DeskAvailabilityService deskAvailabilityService;
    private final AccessControlService accessControlService;

    /** Ritorna tutti gli spazi approvati visibili nel catalogo. */
    @GetMapping("/spaces")
    @Operation(summary = "Elenca gli spazi approvati nel catalogo")
    public ResponseEntity<List<SpaceDTO>> listSpaces(@AuthenticationPrincipal AuthenticatedUser principal) {
        me(principal);
        return ResponseEntity.ok(spaceManagementService.findAll());
    }

    /**
     * Ritorna il dettaglio di uno spazio approvato.
     *
     * @param spaceId ID dello spazio
     */
    @GetMapping("/spaces/{spaceId}")
    @Operation(summary = "Dettaglio di uno spazio approvato")
    public ResponseEntity<SpaceDTO> getSpace(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        me(principal);
        return ResponseEntity.ok(spaceManagementService.findById(spaceId));
    }

    /**
     * Verifica se uno spazio è chiuso in una data specifica.
     * Ritorna {@code null} se non c'è chiusura programmata per quel giorno.
     *
     * @param spaceId ID dello spazio
     * @param date giorno da controllare
     */
    @GetMapping("/spaces/{spaceId}/closures")
    @Operation(summary = "Verifica chiusura straordinaria per data")
    public ResponseEntity<SpaceClosureDTO> getSpaceClosure(
            @PathVariable Long spaceId,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        me(principal);
        return ResponseEntity.ok(spaceClosureService.findForWorker(spaceId, date).orElse(null));
    }

    /**
     * Ritorna gli slot orari di un desk per una data, con stato di disponibilità.
     * La risposta non viene cachata perché la disponibilità cambia in tempo reale.
     *
     * @param deskID ID del desk
     * @param date giorno di cui visualizzare gli slot
     */
    @GetMapping("/desks/{deskID}/slots")
    @Operation(summary = "Elenca slot e disponibilità di una postazione")
    public ResponseEntity<List<SlotStatusDTO>> getDeskSlots(
            @PathVariable Long deskID,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        me(principal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(deskAvailabilityService.getSlotAvailability(deskID, date));
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

