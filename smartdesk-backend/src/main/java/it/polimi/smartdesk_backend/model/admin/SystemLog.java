package it.polimi.smartdesk_backend.model.admin;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Riga audit append-only: attore, azione, severità, IP e timestamp (non WORM/legal hold). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logID;

    private String actorRole;
    private Long actorID;
    private String action;
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private LogLevel severity;

    private String ipAddress;

    /** Rappresentazione testuale per log e dump, senza effetti sul database. */
    public String toAuditString() {
        return String.format("[%s][%s] actor=%s/%s ip=%s action=%s",
                timestamp,
                severity,
                actorRole,
                actorID,
                ipAddress,
                action);
    }
}

