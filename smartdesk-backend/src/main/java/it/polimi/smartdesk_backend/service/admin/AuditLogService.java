package it.polimi.smartdesk_backend.service.admin;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.admin.SystemLog;
import it.polimi.smartdesk_backend.repository.admin.SystemLogRepository;
import lombok.RequiredArgsConstructor;

/** Append-only su {@code system_log}: azioni amministrative e di sicurezza con ruolo attore e IP. */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SystemLogRepository systemLogRepository;

    /** Scrive una riga append-only: ruolo attore (o {@code SYSTEM} se {@code actorRole} nullo), messaggio libero, severità default {@link LogLevel#INFO} se {@code severity} è nullo. */
    @Transactional
    public void log(Role actorRole, Long actorID, String action, LogLevel severity, String ipAddress) {
        SystemLog log = new SystemLog();
        log.setActorRole(actorRole == null ? "SYSTEM" : actorRole.name());
        log.setActorID(actorID);
        log.setAction(action);
        log.setSeverity(severity == null ? LogLevel.INFO : severity);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        systemLogRepository.save(log);
    }

    /**
     * Restituisce tutte le righe di log mappate in {@link LogDTO}, senza paginazione.
     *
     * @return elenco completo come restituito dal repository
     */
    @Transactional(readOnly = true)
    public List<LogDTO> getLogs() {
        return systemLogRepository.findAll().stream()
                .map(log -> new LogDTO(
                        log.getLogID(),
                        log.getActorRole(),
                        log.getAction(),
                        log.getTimestamp(),
                        log.getSeverity() == null ? null : log.getSeverity().name(),
                        log.getIpAddress()))
                .toList();
    }
}

