package it.polimi.smartdesk_backend.service.admin;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.model.admin.LogLevel;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.admin.SystemLog;
import it.polimi.smartdesk_backend.repository.admin.SystemLogRepository;
import it.polimi.smartdesk_backend.util.audit.AuditAction;

/** Scrittura e lettura log di sistema tramite {@link AuditLogService}. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void auditLogDefaultRoleSeverity() {
        auditLogService.log(null, 7L, AuditAction.USER_LOGGED_IN.getDescription(), null, "10.0.0.5");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());

        SystemLog persistedLog = captor.getValue();
        assertEquals("SYSTEM", persistedLog.getActorRole());
        assertEquals(7L, persistedLog.getActorID());
        assertEquals(AuditAction.USER_LOGGED_IN.getDescription(), persistedLog.getAction());
        assertEquals(LogLevel.INFO, persistedLog.getSeverity());
        assertEquals("10.0.0.5", persistedLog.getIpAddress());
        assertTrue(persistedLog.getTimestamp() != null);
    }

    @Test
    void auditLogExplicitValues() {
        auditLogService.log(Role.HOST, 11L, AuditAction.PROFILE_UPDATED.getDescription(), LogLevel.WARN, "127.0.0.1");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());

        SystemLog persistedLog = captor.getValue();
        assertEquals("HOST", persistedLog.getActorRole());
        assertEquals(LogLevel.WARN, persistedLog.getSeverity());
    }

    @Test
    void logTimelineMapped() {
        SystemLog first = new SystemLog();
        first.setLogID(1L);
        first.setActorRole("WORKER");
        first.setAction("BOOK");
        first.setTimestamp(LocalDateTime.of(2026, 2, 1, 9, 0));
        first.setSeverity(LogLevel.INFO);
        first.setIpAddress("1.2.3.4");

        SystemLog second = new SystemLog();
        second.setLogID(2L);
        second.setActorRole("SYSTEM");
        second.setAction("MAINTENANCE");
        second.setTimestamp(LocalDateTime.of(2026, 2, 1, 10, 0));
        second.setSeverity(null);
        second.setIpAddress("5.6.7.8");

        when(systemLogRepository.findAll()).thenReturn(List.of(first, second));

        List<LogDTO> result = auditLogService.getLogs();

        assertEquals(2, result.size());

        LogDTO dto1 = result.get(0);
        assertEquals(1L, dto1.getLogID());
        assertEquals("WORKER", dto1.getActorRole());
        assertEquals("INFO", dto1.getSeverity());

        LogDTO dto2 = result.get(1);
        assertEquals(2L, dto2.getLogID());
        assertNull(dto2.getSeverity());
    }

    @Test
    void emptyTimeline() {
        when(systemLogRepository.findAll()).thenReturn(List.of());

        List<LogDTO> result = auditLogService.getLogs();

        assertTrue(result.isEmpty());
    }
}

