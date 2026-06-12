package it.polimi.smartdesk_backend.service.security;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.polimi.smartdesk_backend.dto.auth.AuthResponseDTO;
import it.polimi.smartdesk_backend.dto.auth.HostRegisterDTO;
import it.polimi.smartdesk_backend.dto.auth.LoginRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RefreshTokenRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.RegisterRequestDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ForbiddenException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.RefreshToken;
import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.user.RefreshTokenRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.admin.AuditLogService;
import it.polimi.smartdesk_backend.service.admin.SysAdminNotificationService;

/** Registrazione, login, refresh token e casi errore (credenziali, ruoli, host in attesa). */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SysAdminNotificationService sysAdminNotificationService;

    @InjectMocks
    private AuthService authService;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setEmail("mario@test.it");
        worker.setPassword("hashed_pw");
        worker.setName("Mario");
        worker.setActive(true);
        lenient().when(tokenService.generateAccessToken(any())).thenReturn("signed-access-token");
        lenient().when(refreshTokenRepository.findByUser_IdAndRevoked(any(Long.class), anyBoolean()))
                .thenReturn(List.of());
    }

    @Test
    void workerRegistrationWithToken() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setName("Mario");
        req.setSurname("Rossi");
        req.setEmail("mario@test.it");
        req.setPassword("password12");
        req.setRole("WORKER");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password12")).thenReturn("hashed_pw");
        when(userRepository.save(any())).thenReturn(worker);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO result = authService.register(req, null);

        assertNotNull(result.getAccessToken());
        assertEquals("WORKER", result.getRole());
        assertEquals(1L, result.getUserID());
    }

    @Test
    void duplicateEmailInRegistration() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("pass");
        req.setRole("WORKER");
        req.setName("Mario");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> authService.register(req, null));
        assertNotNull(exception);
        verify(userRepository, never()).save(any());
    }

    @Test
    void publicRegisterBlocksSysAdmin() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setName("Admin");
        req.setEmail("admin@test.it");
        req.setPassword("pass");
        req.setRole("SYS_ADMIN");

        when(userRepository.findByEmail("admin@test.it")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> authService.register(req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void publicRegisterBlocksTechnician() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setName("Tech");
        req.setEmail("tech@test.it");
        req.setPassword("pass");
        req.setRole("TECHNICIAN");

        when(userRepository.findByEmail("tech@test.it")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> authService.register(req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void technicianOnlyFromHost() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setName("Tech");
        req.setEmail("tech@test.it");
        req.setPassword("pass");
        req.setRole("TECHNICIAN");

        when(userRepository.findByEmail("tech@test.it")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> authService.register(req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminAliasDoesNotPassInRegister() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setName("Root");
        req.setEmail("root@test.it");
        req.setPassword("pass");
        req.setRole("ADMIN");

        when(userRepository.findByEmail("root@test.it")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> authService.register(req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginOkWithValidCredentials() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("pass");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("pass", "hashed_pw")).thenReturn(true);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO result = authService.login(req, null);

        assertNotNull(result.getAccessToken());
        assertEquals("WORKER", result.getRole());
    }

    @Test
    void loginNormalizesEmailCaseInsensitive() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("  Mario@Test.IT ");
        req.setPassword("pass");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("pass", "hashed_pw")).thenReturn(true);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO result = authService.login(req, null);

        assertNotNull(result.getAccessToken());
        verify(userRepository).findByEmail("mario@test.it");
    }

    @Test
    void loginRevokesOldRefresh() {
        RefreshToken token = new RefreshToken();
        token.setUser(worker);
        token.setRevoked(false);
        token.setToken(UUID.randomUUID());
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("pass");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("pass", "hashed_pw")).thenReturn(true);
        when(refreshTokenRepository.findByUser_IdAndRevoked(1L, false)).thenReturn(List.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.login(req, null);

        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    void wrongPasswordLogin() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("wrong");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> authService.login(req, null));
        assertNotNull(exception);
    }

    @Test
    void unknownEmailLoginUsesGenericCredentialsError() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("missing@test.it");
        req.setPassword("pass");

        when(userRepository.findByEmail("missing@test.it")).thenReturn(Optional.empty());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> authService.login(req, null));
        assertNotNull(exception);
    }

    // Il login deve usare passwordEncoder.matches; una stringa uguale all'hash salvato non basta.
    @Test
    void loginDoesNotAcceptPlaintextIfHashFails() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("hashed_pw");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("hashed_pw", "hashed_pw")).thenReturn(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> authService.login(req, null));
        assertNotNull(exception);
    }

    @Test
    void disabledAccountLogin() {
        worker.setActive(false);
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("mario@test.it");
        req.setPassword("pass");

        when(userRepository.findByEmail("mario@test.it")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("pass", "hashed_pw")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> authService.login(req, null));
        assertNotNull(exception);
    }

    @Test
    void quickCheck_hostCannotLoginIfPendingApproval() {
        Host host = new Host();
        host.setId(44L);
        host.setName("Pending host");
        host.setEmail("pending@host.test");
        host.setPassword("hash");
        host.setApproved(false);
        host.setActive(true);

        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("pending@host.test");
        req.setPassword("pass");

        when(userRepository.findByEmail("pending@host.test")).thenReturn(Optional.of(host));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> authService.login(req, null));
    }

    @Test
    void logoutRevokesRefresh() {
        RefreshToken token = new RefreshToken();
        token.setUser(worker);
        token.setRevoked(false);
        token.setToken(UUID.randomUUID());
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(refreshTokenRepository.findByUser_IdAndRevoked(1L, false)).thenReturn(List.of(token));

        authService.logout(1L, null);

        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    void logoutNonexistentUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> authService.logout(99L, null));
        assertNotNull(exception);
    }

    @Test
    void validRefreshReturnsWorker() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setToken(UUID.randomUUID());
        token.setUser(worker);
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshTokenRequestDTO req = new RefreshTokenRequestDTO();
        req.setRefreshToken(token.getToken());

        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO response = authService.refresh(req, "127.0.0.1");

        assertEquals("WORKER", response.getRole());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void expiredRefreshBusinessRule() {
        RefreshToken expired = new RefreshToken();
        expired.setRevoked(false);
        expired.setToken(UUID.randomUUID());
        expired.setUser(worker);
        expired.setExpiryDate(Instant.now().minusSeconds(20));

        RefreshTokenRequestDTO req = new RefreshTokenRequestDTO();
        req.setRefreshToken(expired.getToken());

        when(refreshTokenRepository.findByToken(expired.getToken())).thenReturn(Optional.of(expired));

        assertThrows(BusinessRuleException.class, () -> authService.refresh(req, null));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void sysAdminProfileForSelf() {
        SysAdmin admin = new SysAdmin();
        admin.setId(10L);
        admin.setEmail("admin@test.it");
        admin.setPassword("hashed");

        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertEquals(1L, authService.getUserProfileForRequester(1L, 10L).getUserID());
    }

    @Test
    void disabledRequesterProfile() {
        worker.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> authService.getUserProfileForRequester(1L, 1L));
        assertNotNull(exception);
    }

    @Test
    void registerHostNotifiesAdmin() {
        Host host = new Host();
        host.setId(42L);
        host.setEmail("biz@test.it");
        host.setName("Luigi");
        host.setApproved(false);

        HostRegisterDTO req = new HostRegisterDTO();
        req.setName("Luigi");
        req.setSurname("Verdi");
        req.setEmail("biz@test.it");
        req.setPassword("password12");
        req.setNameStructure("ACME Srl");
        req.setVatNumber("IT123");
        req.setDescription("Centro coworking");

        when(userRepository.findByEmail("biz@test.it")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password12")).thenReturn("hashed_pw");
        when(userRepository.save(any(Host.class))).thenReturn(host);

        AuthResponseDTO result = authService.registerHost(req, null);

        assertNull(result.getAccessToken());
        assertNull(result.getRefreshToken());
        assertEquals(42L, result.getUserID());
        assertEquals("HOST", result.getRole());
        verify(refreshTokenRepository, never()).save(any());
        verify(sysAdminNotificationService, times(1)).notifyAdminsOfPendingHost(host);
    }
}
