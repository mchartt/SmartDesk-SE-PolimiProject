package it.polimi.smartdesk_backend.service.security;
import it.polimi.smartdesk_backend.mapper.UserProfileMapper;

import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.auth.AuthResponseDTO;
import it.polimi.smartdesk_backend.dto.auth.HostRegisterDTO;
import it.polimi.smartdesk_backend.dto.auth.LoginRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RefreshTokenRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RegisterRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.RefreshToken;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.user.RefreshTokenRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import it.polimi.smartdesk_backend.util.audit.AuditAction;
import it.polimi.smartdesk_backend.service.admin.AuditLogService;
import it.polimi.smartdesk_backend.service.admin.SysAdminNotificationService;

/** Registrazione worker/host, login, refresh token e revoca sessioni precedenti su nuovo accesso. */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Durata refresh (7 giorni). Aumentare solo se il prodotto accetta sessioni più lunghe. */
    private static final long REFRESH_TOKEN_TTL_SECONDS = 60L * 60L * 24L * 7L;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final TokenService tokenService;
    private final SysAdminNotificationService sysAdminNotificationService;

    /** Registra un worker o tecnico: email normalizzata, password hashata e emissione token di accesso. */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        ensureEmailAvailable(email);
        
        Role role = parseRole(request.getRole());
        User user = buildUserForRole(role, request);
        user.setEmail(email);
        
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        User savedUser = userRepository.save(user);
        auditLogService.log(savedUser.getRole(), savedUser.getId(), AuditAction.USER_REGISTERED.getDescription(), LogLevel.AUDIT, ipAddress);
        return createAuthResponse(savedUser);
    }

    /** Flusso host self-service: {@code approved=false} finché sysadmin non dà OK; notifica piattaforma se abilitata. */
    @Transactional
    public AuthResponseDTO registerHost(HostRegisterDTO request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        ensureEmailAvailable(email);
        Host host = new Host();
        host.setName(request.getName());
        host.setSurname(request.getSurname());
        host.setEmail(email);
        host.setPassword(passwordEncoder.encode(request.getPassword()));
        host.setNameStructure(request.getNameStructure());
        host.setVatNumber(request.getVatNumber());
        host.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        host.setApproved(false);
        Host savedHost = userRepository.save(host);
        auditLogService.log(savedHost.getRole(), savedHost.getId(), AuditAction.HOST_REGISTERED_PENDING.getDescription(),
                LogLevel.AUDIT, ipAddress);
        sysAdminNotificationService.notifyAdminsOfPendingHost(savedHost);
        return createRegistrationResponseWithoutTokens(savedHost);
    }

    /** Login: normalizza email, verifica hash e blocca account disattivati o host pending; messaggi generici anti-enumeration dove applicabile. */
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request, String ipAddress) {
        String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException(AuthMessage.INVALID_CREDENTIALS.text()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessRuleException(AuthMessage.INVALID_CREDENTIALS.text());
        }
        if (!user.isActive()) {
            throw new BusinessRuleException(AuthMessage.USER_DISABLED.text());
        }
        if (user instanceof Host host && !host.isApproved()) {
            throw new BusinessRuleException(AuthMessage.HOST_PENDING.text());
        }

        auditLogService.log(user.getRole(), user.getId(), AuditAction.USER_LOGGED_IN.getDescription(), LogLevel.AUDIT, ipAddress);
        return createAuthResponse(user);
    }

    /** Rotazione refresh strict: token presente ma revocato/scaduto → errore. Il vecchio refresh viene sempre segnato revocato prima di emetterne uno nuovo (replay mitigation base). */
    @Transactional
    public AuthResponseDTO refresh(RefreshTokenRequestDTO request, String ipAddress) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new NotFoundException(AuthMessage.REFRESH_TOKEN_NOT_FOUND.text()));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new BusinessRuleException(AuthMessage.REFRESH_TOKEN_STALE.text());
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        auditLogService.log(user.getRole(), user.getId(), AuditAction.REFRESH_TOKEN_USED.getDescription(), LogLevel.AUDIT, ipAddress);
        return createAuthResponse(user);
    }

    /** Mappa l'utente in {@link UserProfileDTO} senza controllo sul richiedente; da usare dopo aver ristretto l'accesso. */
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));

        return UserProfileMapper.fromUser(user);
    }

    /** Profilo con gate: self oppure {@link Role#SYS_ADMIN}; chi altro riceve {@link NotFoundException} finta (anti-enumeration). */
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileForRequester(Long targetUserID, Long requesterID) {
        User requester = findActiveUser(requesterID);
        if (!requester.getId().equals(targetUserID) && requester.getRole() != Role.SYS_ADMIN) {
            throw new NotFoundException(ResourceMessage.userNotFound(targetUserID));
        }
        return getUserProfile(targetUserID);
    }

    /** Logout: revoca tutti i refresh token non revocati dell'utente. */
    @Transactional
    public void logout(Long userID, String ipAddress) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));

        List<RefreshToken> tokens = refreshTokenRepository.findByUser_IdAndRevoked(userID, false);
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        auditLogService.log(user.getRole(), user.getId(), AuditAction.USER_LOGGED_OUT.getDescription(), LogLevel.AUDIT, ipAddress);
    }

    /** Stesso gate di {@link #getUserProfileForRequester}: solo self o admin possono chiudere sessione altrui; poi {@link #logout}. */
    @Transactional
    public void logoutForRequester(Long targetUserID, Long requesterID, String ipAddress) {
        User requester = findActiveUser(requesterID);
        if (!requester.getId().equals(targetUserID) && requester.getRole() != Role.SYS_ADMIN) {
            throw new NotFoundException(ResourceMessage.userNotFound(targetUserID));
        }
        logout(targetUserID, ipAddress);
    }

    private User findActiveUser(Long userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));
        if (!user.isActive()) {
            throw new ForbiddenException(AuthMessage.forbiddenUserDisabled(userID));
        }
        return user;
    }

    private void ensureEmailAvailable(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            throw new BusinessRuleException(AuthMessage.EMAIL_ALREADY_REGISTERED.text());
        });
    }

    private User buildUserForRole(Role role, RegisterRequestDTO request) {
        switch (role) {
            case WORKER -> {
                Worker worker = new Worker();
                worker.setName(request.getName().trim());
                worker.setSurname(request.getSurname().trim());
                String bio = request.getBio() != null ? request.getBio().trim() : "";
                if (bio.isEmpty() && request.getCompany() != null && !request.getCompany().isBlank()) {
                    bio = request.getCompany().trim();
                }
                worker.setBio(bio);
                return worker;
            }
            case TECHNICIAN ->
                throw new BusinessRuleException(AuthMessage.TECHNICIAN_REGISTER_VIA_HOST.text());
            case SYS_ADMIN -> throw new BusinessRuleException(AuthMessage.SYS_ADMIN_REGISTER_FORBIDDEN.text());
            case HOST -> throw new BusinessRuleException(AuthMessage.HOST_REGISTER_USE_DEDICATED.text());
            default -> throw new BusinessRuleException(AuthMessage.UNSUPPORTED_ROLE.text());
        }
    }

    private Role parseRole(String value) {
        if (value == null) {
            throw new BusinessRuleException(AuthMessage.ROLE_REQUIRED.text());
        }
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new BusinessRuleException(AuthMessage.unsupportedRoleValue(value));
    }

    private AuthResponseDTO createAuthResponse(User user) {
        Instant accessExpiry = tokenService.getAccessTokenExpiry();
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUser_IdAndRevoked(user.getId(), false);
        activeTokens.forEach(token -> token.setRevoked(true));
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS));
        refreshTokenRepository.save(refreshToken);

        String accessToken = tokenService.generateAccessToken(user);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken().toString());
        response.setExpiresIn(accessExpiry);
        response.setUserID(user.getId());
        response.setRole(user.getRole().name());
        return response;
    }

    private AuthResponseDTO createRegistrationResponseWithoutTokens(User user) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setTokenType(null);
        response.setUserID(user.getId());
        response.setRole(user.getRole().name());
        return response;
    }
}

