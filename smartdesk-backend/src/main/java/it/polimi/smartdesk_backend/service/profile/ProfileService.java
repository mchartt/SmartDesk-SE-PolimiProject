package it.polimi.smartdesk_backend.service.profile;
import it.polimi.smartdesk_backend.mapper.UserProfileMapper;

import it.polimi.smartdesk_backend.util.message.AdminMessage;
import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.util.policy.PasswordPolicy;
import it.polimi.smartdesk_backend.util.audit.AuditAction;
import it.polimi.smartdesk_backend.service.admin.AuditLogService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

/** Lettura e aggiornamento profilo, cambio password e disattivazione account per utente autenticato. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /** Lettura profilo: nessuna maschera qui—il controller ha già autenticato. */
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long userID) {
        User user = findUser(userID);
        return UserProfileMapper.fromUser(user);
    }

    /** Aggiorna l'anagrafica in modo incrementale: persiste solo se cambiano i campi, registra audit log e notifica l'utente con l'elenco dei campi modificati. */
    @Transactional
    public UserProfileDTO updateProfile(Long userID, String name, String surname, String email) {
        User user = findUser(userID);
        String trimmedEmail = email.toLowerCase().trim();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, userID)) {
            throw new BusinessRuleException(AuthMessage.EMAIL_ALREADY_REGISTERED.text());
        }

        List<String> changedParts = new ArrayList<>();
        if (!sameTrimmed(user.getName(), name)) {
            user.setName(name.trim());
            changedParts.add("nome");
        }
        if (!sameTrimmed(user.getSurname(), surname)) {
            user.setSurname(surname.trim());
            changedParts.add("cognome");
        }
        if (!user.getEmail().equals(trimmedEmail)) {
            user.setEmail(trimmedEmail);
            changedParts.add("email");
        }

        if (!changedParts.isEmpty()) {
            userRepository.save(user);
            auditLogService.log(user.getRole(), userID, AuditAction.PROFILE_UPDATED.getDescription(), LogLevel.INFO, null);
            notificationService.sendUserNotification(userID,
                    "Profilo aggiornato (" + String.join(", ", changedParts) + ").");
        }

        return UserProfileMapper.fromUser(user);
    }

    /** Confronto case-insensitive con trim per evitare persistenza inutile quando i valori non cambiano. */
    private static boolean sameTrimmed(String a, String b) {
        String ta = a == null ? "" : a.trim();
        String tb = b == null ? "" : b.trim();
        return ta.equalsIgnoreCase(tb);
    }

    /** Password change classico: bcrypt match sulla vecchia; nuova password come registrazione ({@link PasswordPolicy}). */
    @Transactional
    public void changePassword(Long userID, String currentPassword, String newPassword) {
        User user = findUser(userID);

        boolean matches = passwordEncoder.matches(currentPassword, user.getPassword());

        if (!matches) {
            throw new BusinessRuleException(AdminMessage.CURRENT_PASSWORD_WRONG.text());
        }
        if (!PasswordPolicy.isStrong(newPassword)) {
            throw new BusinessRuleException(AdminMessage.NEW_PASSWORD_POLICY_VIOLATION.text());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(user.getRole(), userID, AuditAction.PASSWORD_CHANGED.getDescription(), LogLevel.AUDIT, null);
    }

    /** Soft-delete self-service: {@code active=false}. Non cancella righe correlate (prenotazioni storiche, ecc.)—decisione data model. */
    @Transactional
    public void deleteAccount(Long userID, String ipAddress) {
        User user = findUser(userID);
        user.setActive(false);
        userRepository.save(user);
        auditLogService.log(user.getRole(), userID, AuditAction.ACCOUNT_DEACTIVATED_BY_USER.getDescription(), LogLevel.AUDIT, ipAddress);
    }

    /** Helper interno: {@link NotFoundException} se PK assente. */
    private User findUser(Long userID) {
        return userRepository.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));
    }

}

