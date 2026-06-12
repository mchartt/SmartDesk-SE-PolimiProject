package it.polimi.smartdesk_backend.controller.booking;
import lombok.RequiredArgsConstructor;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;

/** Lettura prenotazione per ID con controlli ruolo (worker titolare, host spazio, SYS_ADMIN). */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Prenotazioni", description = "Lettura dettaglio prenotazione con controlli per ruolo e titolarità.")
@RequiredArgsConstructor
public class BookingController {

    private final BookingQueryService bookingQueryService;
    private final AccessControlService accessControlService;

    /** Restituisce il dettaglio della prenotazione se il richiedente è autorizzato; accesso negato mascherato come 404. */
    @GetMapping("/{bookingId}")
    @Operation(summary = "Dettaglio prenotazione per ID con controlli di accesso per ruolo")
    public ResponseEntity<BookingDTO> getBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(bookingQueryService.findByIdForUser(bookingId, requester.getUserId(), requester.getRole()));
    }
}


