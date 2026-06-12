package it.polimi.smartdesk_backend.controller.host;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import lombok.RequiredArgsConstructor;

/** L'host vede tutte le prenotazioni sui propri spazi. */
@RestController
@RequestMapping("/api/hosts/bookings")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Prenotazioni host", description = "Consultazione prenotazioni sui propri spazi.")
@RequiredArgsConstructor
public class HostBookingController {

    private final BookingQueryService bookingQueryService;
    private final AccessControlService accessControlService;

    /** Ritorna tutte le prenotazioni attive e passate sui desk degli spazi dell'host autenticato. */
    @GetMapping
    @Operation(summary = "Elenca le prenotazioni sui propri spazi")
    public ResponseEntity<List<BookingDTO>> getHostBookings(@AuthenticationPrincipal AuthenticatedUser principal) {
        var host = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(bookingQueryService.getBookingsByHost(host.getUserId()));
    }
}

