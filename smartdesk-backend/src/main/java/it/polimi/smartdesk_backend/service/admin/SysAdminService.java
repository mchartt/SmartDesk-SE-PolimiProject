package it.polimi.smartdesk_backend.service.admin;
import it.polimi.smartdesk_backend.mapper.UserProfileMapper;

import it.polimi.smartdesk_backend.util.message.AdminMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.model.user.UserModerationAction;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;

/** Moderazione utenti/host/spazi e lettura audit log per profilo SYS_ADMIN. */
@Service
@RequiredArgsConstructor
public class SysAdminService {

    /** Utenti generici: ban/reactivate/list. */
    private final UserRepository userRepo;
    /** Host: usati per approve/reject e per la coda pending. */
    private final HostRepository hostRepo;
    /** Spazi: approve/reject/force-close passano da qui. */
    private final SpaceRepository spaceRepo;
    /** Append-only log lato applicazione (non sostituisce SIEM). */
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * Applica un'azione di moderazione su un utente (BAN o REACTIVATE).
     *
     * @param action BAN o REACTIVATE; host/worker/sysadmin con regole diverse su {@code approved}
     * @throws NotFoundException utente inesistente
     * @throws BusinessRuleException azione non applicabile allo stato corrente
     */
    @Transactional
    public void moderateUser(Long adminUserID, Long userID, UserModerationAction action, String ipAddress) {
        switch (action) {
            case BAN -> banUser(adminUserID, userID, ipAddress);
            case REACTIVATE -> reactivateUser(userID, ipAddress);
        }
    }

    /**
     * Disattiva un utente ({@code active = false}).
     * L'admin non può bannare se stesso quando l'identità dell'attore è nota.
     *
     * @param adminUserID utente che esegue l'azione (da JWT); se {@code null} il controllo self-ban non gira
     * @param userID      utente da disattivare
     * @param ipAddress   client IP per audit (può essere {@code null})
     */
    @Transactional
    public void banUser(Long adminUserID, Long userID, String ipAddress) {
        if (adminUserID != null && adminUserID.equals(userID)) {
            throw new BusinessRuleException(AdminMessage.ADMIN_CANNOT_BAN_SELF.text());
        }
        User user = userRepo.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));
        user.setActive(false);
        userRepo.save(user);
        auditLogService.log(Role.SYS_ADMIN, null, "Utente disattivato: " + userID, LogLevel.AUDIT, ipAddress);
    }


    /**
     * Variante senza attore: nessun confronto admin/target.
     * Riservata a seed, script e migrazioni; l'API HTTP usa {@link #banUser(Long, Long, String)}.
     *
     * @param userID    utente da disattivare
     * @param ipAddress IP opzionale per traccia in log
     */
    @Transactional
    public void banUser(Long userID, String ipAddress) {
        banUser(null, userID, ipAddress);
    }
    /** Riattiva un utente bannato impostando {@code active=true}; non modifica password o token. */
    @Transactional
    public void reactivateUser(Long userID, String ipAddress) {
        User user = userRepo.findById(userID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.userNotFound(userID)));
        user.setActive(true);
        userRepo.save(user);
        auditLogService.log(Role.SYS_ADMIN, null, "Utente riattivato: " + userID, LogLevel.AUDIT, ipAddress);
    }

    /** Approva un host impostando {@code approved=true} e registra l'azione in audit log. */
    @Transactional
    public void approveHost(Long hostID, String ipAddress) {
        Host host = hostRepo.findById(hostID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.hostNotFound(hostID)));
        host.setApproved(true);
        hostRepo.save(host);
        auditLogService.log(Role.SYS_ADMIN, null, "Host approvato: " + hostID, LogLevel.AUDIT, ipAddress);
    }

    /** Respinge un host con {@code approved=false} e {@code active=false}; escluso da directory e code operative. */
    @Transactional
    public void rejectHost(Long hostID, String ipAddress) {
        Host host = hostRepo.findById(hostID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.hostNotFound(hostID)));
        host.setApproved(false);
        host.setActive(false);
        hostRepo.save(host);
        auditLogService.log(Role.SYS_ADMIN, null, "Host rifiutato: " + hostID, LogLevel.WARN, ipAddress);
    }

    /** Approva uno spazio impostando {@code approved=true} e notifica l'host. */
    @Transactional
    public void approveSpace(Long spaceId, String ipAddress) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        space.setApproved(true);
        spaceRepo.save(space);
        notificationService.notifySpaceDecision(space.getHostID(), space.getName(), "approvato");
        auditLogService.log(Role.SYS_ADMIN, null, "Spazio approvato: " + spaceId, LogLevel.AUDIT, ipAddress);
    }

    /** Respinge uno spazio impostando {@code approved=false}; il record resta in database per storico. */
    @Transactional
    public void rejectSpace(Long spaceId, String ipAddress) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        space.setApproved(false);
        spaceRepo.save(space);
        notificationService.notifySpaceDecision(space.getHostID(), space.getName(), "rifiutato");
        auditLogService.log(Role.SYS_ADMIN, null, "Spazio rifiutato: " + spaceId, LogLevel.WARN, ipAddress);
    }

    /**
     * Elenca gli utenti per la directory admin, escludendo gli host ancora in pending.
     * Gli host in attesa sono in {@link #getPendingHosts()}.
     *
     * @return lista potenzialmente grande; ordinamento = quello del repository (non garantito stabile)
     */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers() {
        return userRepo.findAll().stream()
                .filter(SysAdminService::includeUserInAdminDirectory)
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    /** Esclude gli host non ancora approvati dalla directory globale admin. */
    private static boolean includeUserInAdminDirectory(User user) {
        if (user instanceof Host h) {
            return h.isApproved();
        }
        return true;
    }

    /** Elenca gli host in coda di approvazione: non approvati e ancora {@code active}. */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getPendingHosts() {
        return hostRepo.findAll().stream()
                .filter(h -> !h.isApproved() && h.isActive())
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    /** Elenco degli host che sono stati rifiutati (non approvati e disattivati). */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getRejectedHosts() {
        return hostRepo.findAll().stream()
                .filter(h -> !h.isApproved() && !h.isActive())
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    /** Forza la chiusura di uno spazio approvato revocando l'approvazione e registrando l'azione in audit log. */
    @Transactional
    public void forceCloseSpace(Long spaceId, String ipAddress) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));
        space.setApproved(false);
        spaceRepo.save(space);
        notificationService.notifySpaceDecision(space.getHostID(), space.getName(), "chiuso forzatamente");
        auditLogService.log(Role.SYS_ADMIN, null, "Spazio chiuso forzatamente: " + spaceId, LogLevel.AUDIT, ipAddress);
    }

    /** Restituisce l'intero audit log di sistema senza paginazione. */
    public List<LogDTO> getSystemLogs() {
        return auditLogService.getLogs();
    }

    /** Mappa un utente in {@link UserProfileDTO} per la vista admin. */
    private UserProfileDTO toProfile(User user) {
        return UserProfileMapper.adminFromUser(user);
    }
}

