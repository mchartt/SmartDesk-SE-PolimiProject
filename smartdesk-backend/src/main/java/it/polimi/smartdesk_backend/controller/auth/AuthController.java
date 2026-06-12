package it.polimi.smartdesk_backend.controller.auth;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polimi.smartdesk_backend.dto.auth.AuthResponseDTO;
import it.polimi.smartdesk_backend.dto.auth.HostRegisterDTO;
import it.polimi.smartdesk_backend.dto.auth.LoginRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RefreshTokenRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RegisterRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.security.AuthService;
import it.polimi.smartdesk_backend.util.support.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Login, registrazione worker/host, refresh token e logout — niente ruolo richiesto (tranne /me). */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione", description = "Registrazione worker/host, login, refresh JWT, profilo e logout.")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccessControlService accessControlService;

    /** Registrazione worker: email univoca; risposta con access + refresh token. */
    @PostMapping("/register")
    @Operation(summary = "Registra un account worker")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, RequestUtils.getClientIp(httpRequest)));
    }

    /** Registrazione host: account {@link it.polimi.smartdesk_backend.model.user.AccountStatus#PENDING_APPROVAL} fino ad approvazione SYS_ADMIN. */
    @PostMapping("/register/host")
    @Operation(summary = "Registra un account host")
    public ResponseEntity<AuthResponseDTO> registerHost(
            @Valid @RequestBody HostRegisterDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerHost(request, RequestUtils.getClientIp(httpRequest)));
    }

    /** Login: credenziali valide → token; IP client registrato per audit sicurezza. */
    @PostMapping("/login")
    @Operation(summary = "Autentica un utente e rilascia i token")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, RequestUtils.getClientIp(httpRequest)));
    }

    /** Rotazione refresh token: revoca il precedente e ritorna nuova coppia access/refresh. */
    @PostMapping("/refresh")
    @Operation(summary = "Ruota il refresh token e rilascia nuovi token")
    public ResponseEntity<AuthResponseDTO> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(request, RequestUtils.getClientIp(httpRequest)));
    }

    /** Profilo utente {@code userID} filtrato in base al richiedente autenticato. */
    @GetMapping("/users/{userID}/profile")
    @Operation(summary = "Legge il profilo per ID utente")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @PathVariable Long userID,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(authService.getUserProfileForRequester(userID, requester.getUserId()));
    }

    /** Profilo dell'utente corrente (subject JWT). */
    @GetMapping("/me")
    @Operation(summary = "Legge il profilo dell'utente autenticato corrente")
    public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        var requester = accessControlService.assertAuthenticated(principal);
        return ResponseEntity.ok(authService.getUserProfileForRequester(requester.getUserId(), requester.getUserId()));
    }

    /** Logout: revoca i refresh token attivi dell'utente. */
    @DeleteMapping("/logout/{userID}")
    @Operation(summary = "Effettua logout e revoca i refresh token attivi")
    public ResponseEntity<Void> logout(
            @PathVariable Long userID,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest) {
        var requester = accessControlService.assertAuthenticated(principal);
        authService.logoutForRequester(userID, requester.getUserId(), RequestUtils.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }
}


