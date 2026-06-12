package it.polimi.smartdesk_backend.service.profile;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.admin.AuditLogService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

/** Aggiornamento profilo worker, password e vincoli di validazione. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProfileService profileService;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setName("Mario");
        worker.setSurname("Rossi");
        worker.setEmail("mario@test.it");
        worker.setPassword("hashed_pw");
        worker.setActive(true);
    }

    @Test
    void profileDtoCorrect() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        UserProfileDTO dto = profileService.getProfile(1L);

        assertEquals(1L, dto.getUserID());
        assertEquals("Mario", dto.getName());
        assertEquals("Rossi", dto.getSurname());
        assertEquals("WORKER", dto.getRole());
    }

    @Test
    void profileUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> profileService.getProfile(99L));
        assertNotNull(exception);
    }

    @Test
    void updateProfileNotifies() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(userRepository.save(any())).thenReturn(worker);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@test.it", 1L)).thenReturn(false);

        UserProfileDTO dto = profileService.updateProfile(1L, "Luigi", "Verdi", "new@test.it");

        assertEquals("Luigi", dto.getName());
        assertEquals("Verdi", dto.getSurname());
        assertEquals("new@test.it", dto.getEmail());
        verify(userRepository).save(worker);
        verify(notificationService).sendUserNotification(eq(1L), anyString());
        verify(auditLogService).log(eq(Role.WORKER), eq(1L), eq("Profilo aggiornato"), eq(LogLevel.INFO), isNull());
    }

    @Test
    void updateProfileUnchangedSkip() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        UserProfileDTO dto = profileService.updateProfile(1L, "Mario", "Rossi", "mario@test.it");

        assertEquals("Mario", dto.getName());
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendUserNotification(any(), any());
    }

    @Test
    void updateProfileEmailTaken() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@test.it", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> profileService.updateProfile(1L, "Mario", "Rossi", "taken@test.it"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordSucceeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("Secret1!", "hashed_pw")).thenReturn(true);
        when(passwordEncoder.encode("Othersec2!")).thenReturn("hashed_new");

        profileService.changePassword(1L, "Secret1!", "Othersec2!");

        assertEquals("hashed_new", worker.getPassword());
        verify(userRepository).save(worker);
    }

    @Test
    void oldPasswordWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> profileService.changePassword(1L, "wrong", "Othersec2!"));
        assertNotNull(exception);
        verify(userRepository, never()).save(any());
    }

    @Test
    void newPasswordTooShort() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("Secret1!", "hashed_pw")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> profileService.changePassword(1L, "Secret1!", "abc"));
        assertNotNull(exception);
    }

    @Test
    void quickCheck() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("Secret1!", "hashed_pw")).thenReturn(true);
        when(passwordEncoder.encode("Secret1!")).thenReturn("rehash_same");

        profileService.changePassword(1L, "Secret1!", "Secret1!");

        assertEquals("rehash_same", worker.getPassword());
        verify(userRepository).save(worker);
    }

    @Test
    void deleteAccountDeactivates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        profileService.deleteAccount(1L, null);

        assertFalse(worker.isActive());
        verify(userRepository).save(worker);
        verify(auditLogService).log(eq(Role.WORKER), eq(1L), eq("Account disattivato (richiesta utente)"), eq(LogLevel.AUDIT),
                isNull());
    }
}
