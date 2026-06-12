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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.space.RoomDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceAmenityPresetDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceRequestDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.host.HostAmenityPresetService;
import it.polimi.smartdesk_backend.service.host.HostRoomService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** CRUD spazi dell'host: ufficio, stanze e preset amenity. */
@RestController
@RequestMapping("/api/hosts")
@PreAuthorize("hasRole('HOST')")
@Tag(name = "Spazi host", description = "CRUD spazi, stanze e preset amenity lato host.")
@RequiredArgsConstructor
public class HostSpaceController {

    private final SpaceManagementService spaceManagementService;
    private final HostRoomService hostRoomService;
    private final HostAmenityPresetService hostAmenityPresetService;
    private final AccessControlService accessControlService;

    /**
     * Crea un nuovo spazio coworking: rimane in attesa di approvazione admin prima di essere visibile.
     *
     * @param request dati dello spazio (nome, indirizzo, orari)
     * @return spazio creato con HTTP 201
     */
    @PostMapping
    @Operation(summary = "Crea un nuovo spazio coworking")
    public ResponseEntity<SpaceDTO> createSpace(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SpaceRequestDTO request) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(spaceManagementService.createSpace(host.getUserId(), request));
    }

    /**
     * Ritorna tutti gli spazi dell'host (approvati e non).
     *
     * @param hostID ID dell'host (deve coincidere con il principal)
     */
    @GetMapping("/{hostID}/spaces")
    @Operation(summary = "Elenca gli spazi dell'host")
    public ResponseEntity<List<SpaceDTO>> getMySpaces(
            @PathVariable Long hostID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        accessControlService.assertHostOwnsPath(principal, hostID, "spaces");
        return ResponseEntity.ok(spaceManagementService.findByHost(hostID));
    }

    /**
     * Aggiorna i dati di uno spazio (nome, indirizzo, orari di apertura).
     *
     * @param spaceId ID dello spazio da aggiornare
     * @param request nuovi dati
     * @return spazio aggiornato
     */
    @PutMapping("/spaces/{spaceId}")
    @Operation(summary = "Aggiorna i dati di uno spazio")
    public ResponseEntity<SpaceDTO> updateSpace(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SpaceRequestDTO request) {
        var host = me(principal);
        return ResponseEntity.ok(spaceManagementService.updateSpaceForHost(host.getUserId(), spaceId, request));
    }

    /**
     * Elimina uno spazio e tutto ciò che contiene (desk, stanze, preset).
     *
     * @param spaceId ID dello spazio da eliminare
     */
    @DeleteMapping("/spaces/{spaceId}")
    @Operation(summary = "Elimina uno spazio e i contenuti associati")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        spaceManagementService.deleteSpaceForHost(host.getUserId(), spaceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elenca le stanze/sale di uno spazio dell'host.
     *
     * @param spaceId ID dello spazio
     */
    @GetMapping("/spaces/{spaceId}/rooms")
    @Operation(summary = "Elenca le stanze di uno spazio")
    public ResponseEntity<List<RoomDTO>> listRooms(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostRoomService.listRoomsForHost(host.getUserId(), spaceId));
    }

    /**
     * Crea una nuova stanza/sala nello spazio.
     *
     * @param spaceId ID dello spazio
     * @param body dati della stanza (nome, piano)
     * @return stanza creata con HTTP 201
     */
    @PostMapping("/spaces/{spaceId}/rooms")
    @Operation(summary = "Crea una stanza in uno spazio")
    public ResponseEntity<RoomDTO> createRoom(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RoomDTO body) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(hostRoomService.createRoomForHost(host.getUserId(), spaceId, body));
    }

    /**
     * Aggiorna i dati di una stanza.
     *
     * @param spaceId ID dello spazio contenitore
     * @param roomId ID della stanza da aggiornare
     * @param body nuovi dati
     */
    @PutMapping("/spaces/{spaceId}/rooms/{roomId}")
    @Operation(summary = "Aggiorna i dati di una stanza")
    public ResponseEntity<RoomDTO> updateRoom(
            @PathVariable Long spaceId,
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RoomDTO body) {
        var host = me(principal);
        return ResponseEntity.ok(hostRoomService.updateRoomForHost(host.getUserId(), spaceId, roomId, body));
    }

    /**
     * Elimina una stanza dallo spazio.
     *
     * @param spaceId ID dello spazio
     * @param roomId ID della stanza da eliminare
     */
    @DeleteMapping("/spaces/{spaceId}/rooms/{roomId}")
    @Operation(summary = "Elimina una stanza dallo spazio")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long spaceId,
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostRoomService.deleteRoomForHost(host.getUserId(), spaceId, roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elenca i preset amenity configurati per uno spazio.
     *
     * @param spaceId ID dello spazio
     */
    @GetMapping("/spaces/{spaceId}/amenity-presets")
    @Operation(summary = "Elenca i preset amenity di uno spazio")
    public ResponseEntity<List<SpaceAmenityPresetDTO>> listAmenityPresets(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        return ResponseEntity.ok(hostAmenityPresetService.listAmenityPresetsForHost(host.getUserId(), spaceId));
    }

    /**
     * Crea un preset amenity per lo spazio.
     *
     * @param spaceId ID dello spazio
     * @param body etichetta e dettagli del preset
     * @return preset creato con HTTP 201
     */
    @PostMapping("/spaces/{spaceId}/amenity-presets")
    @Operation(summary = "Crea un preset amenity per uno spazio")
    public ResponseEntity<SpaceAmenityPresetDTO> createAmenityPreset(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SpaceAmenityPresetDTO body) {
        var host = me(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hostAmenityPresetService.createAmenityPresetForHost(host.getUserId(), spaceId, body));
    }

    /**
     * Aggiorna un preset amenity esistente.
     *
     * @param spaceId ID dello spazio
     * @param presetId ID del preset da aggiornare
     * @param body nuovi dati
     */
    @PutMapping("/spaces/{spaceId}/amenity-presets/{presetId}")
    @Operation(summary = "Aggiorna un preset amenity")
    public ResponseEntity<SpaceAmenityPresetDTO> updateAmenityPreset(
            @PathVariable Long spaceId,
            @PathVariable Long presetId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SpaceAmenityPresetDTO body) {
        var host = me(principal);
        return ResponseEntity.ok(hostAmenityPresetService.updateAmenityPresetForHost(host.getUserId(), spaceId, presetId, body));
    }

    /**
     * Elimina un preset amenity dallo spazio.
     *
     * @param spaceId ID dello spazio
     * @param presetId ID del preset da eliminare
     */
    @DeleteMapping("/spaces/{spaceId}/amenity-presets/{presetId}")
    @Operation(summary = "Elimina un preset amenity dallo spazio")
    public ResponseEntity<Void> deleteAmenityPreset(
            @PathVariable Long spaceId,
            @PathVariable Long presetId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var host = me(principal);
        hostAmenityPresetService.deleteAmenityPresetForHost(host.getUserId(), spaceId, presetId);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser me(AuthenticatedUser principal) {
        return accessControlService.assertAuthenticated(principal);
    }
}

