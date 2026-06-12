package it.polimi.smartdesk_backend.controller.worker;

import java.util.List;
import java.util.Map;

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
import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.booking.BookingRequestDTO;
import it.polimi.smartdesk_backend.dto.booking.RescheduleBookingDTO;
import it.polimi.smartdesk_backend.dto.booking.SearchCriteriaDTO;
import it.polimi.smartdesk_backend.dto.booking.SlotStatusDTO;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.BookingCancellationService;
import it.polimi.smartdesk_backend.service.booking.BookingCreationService;
import it.polimi.smartdesk_backend.service.booking.BookingEndSessionService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.booking.BookingWorkerHistoryService;
import it.polimi.smartdesk_backend.service.booking.DeskAvailabilityService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Il worker cerca desk liberi, prenota, sposta o cancella e consulta lo storico. */
@RestController
@RequestMapping("/api/workers/bookings")
@PreAuthorize("hasRole('WORKER')")
@Tag(name = "Prenotazioni worker", description = "Ricerca, creazione, modifica e cancellazione prenotazioni.")
@RequiredArgsConstructor
public class WorkerBookingController {

    private final BookingCreationService bookingCreationService;
    private final BookingCancellationService bookingCancellationService;
    private final BookingEndSessionService bookingEndSessionService;
    private final BookingQueryService bookingQueryService;
    private final BookingWorkerHistoryService bookingWorkerHistoryService;
    private final DeskAvailabilityService deskAvailabilityService;
    private final AccessControlService accessControlService;

    /**
     * Crea una nuova prenotazione per il worker autenticato.
     *
     * @param bookingDTO dati della prenotazione (desk, giorno, orario)
     * @return prenotazione creata con HTTP 201
     */
    @PostMapping
    @Operation(summary = "Crea una nuova prenotazione")
    public ResponseEntity<BookingDTO> bookDesk(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody BookingRequestDTO bookingDTO) {
        var worker = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingCreationService.createBooking(worker.getUserId(), bookingDTO));
    }

    /** Ritorna tutte le prenotazioni (passate e future) del worker autenticato. */
    @GetMapping
    @Operation(summary = "Elenca le prenotazioni del worker")
    public ResponseEntity<List<BookingDTO>> getMyBookings(@AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(bookingQueryService.getBookingsByWorker(worker.getUserId()));
    }

    /** Lista le prenotazioni completate per cui il worker può ancora lasciare una recensione. */
    @GetMapping("/review-eligible")
    @Operation(summary = "Elenca le prenotazioni idonee a recensione")
    public ResponseEntity<List<BookingDTO>> getReviewEligibleBookings(@AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(bookingQueryService.getReviewEligibleBookings(worker.getUserId()));
    }

    /**
     * Sposta una prenotazione esistente a un altro slot orario o giorno.
     *
     * @param bookingId ID della prenotazione da spostare
     * @param request nuovi dati di data/orario
     * @return prenotazione aggiornata
     */
    @PatchMapping("/{bookingId}")
    @Operation(summary = "Sposta una prenotazione esistente")
    public ResponseEntity<BookingDTO> rescheduleBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RescheduleBookingDTO request) {
        var worker = me(principal);
        request.setBookingId(bookingId);
        return ResponseEntity.ok(bookingCreationService.rescheduleBooking(worker.getUserId(), bookingId, request));
    }

    /**
     * Cancella una prenotazione del worker (solo se ancora futura o attiva).
     *
     * @param bookingId ID della prenotazione da cancellare
     */
    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancella una prenotazione del worker")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        bookingCancellationService.removeBookingForUser(bookingId, worker.getUserId(), Role.WORKER);
        return ResponseEntity.noContent().build();
    }

    /**
     * Termina anticipatamente la sessione in corso e libera il desk.
     *
     * @param bookingId ID della prenotazione attiva
     * @return prenotazione aggiornata con l'orario di fine effettivo
     */
    @PostMapping("/{bookingId}/leave")
    @Operation(summary = "Termina anticipatamente una sessione di lavoro")
    public ResponseEntity<BookingDTO> leaveDesk(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        return ResponseEntity.ok(bookingEndSessionService.endSessionForWorker(worker.getUserId(), bookingId));
    }

    /**
     * Elimina tutte le prenotazioni passate del worker dallo storico.
     *
     * @return mappa con il numero di record eliminati
     */
    @DeleteMapping("/history")
    @Operation(summary = "Svuota lo storico prenotazioni passate")
    public ResponseEntity<Map<String, Integer>> clearBookingHistory(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var worker = me(principal);
        int deleted = bookingWorkerHistoryService.clearPastBookingsForWorker(worker.getUserId());
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    /**
     * Cerca desk disponibili in base ai criteri forniti (città, data, orario, amenity).
     *
     * @param criteria filtri di ricerca
     * @return lista di desk che rispettano tutti i criteri
     */
    @PostMapping("/search")
    @Operation(summary = "Cerca postazioni disponibili per criteri")
    public ResponseEntity<List<DeskDTO>> searchDesks(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SearchCriteriaDTO criteria) {
        me(principal);
        return ResponseEntity.ok(deskAvailabilityService.searchDesks(criteria));
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

