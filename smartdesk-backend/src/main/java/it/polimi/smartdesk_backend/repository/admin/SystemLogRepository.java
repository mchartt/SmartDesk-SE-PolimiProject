package it.polimi.smartdesk_backend.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.admin.SystemLog;

/** Persistenza audit trail amministrativo ({@link SystemLog}). */
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
}

