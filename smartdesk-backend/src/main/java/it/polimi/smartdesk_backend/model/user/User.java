package it.polimi.smartdesk_backend.model.user;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Column;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import it.polimi.smartdesk_backend.model.admin.SysAdmin;

/** Radice JPA single-table per worker/host/technician/sysadmin: credenziali, {@code active}, {@code version} optimistic lock. */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String surname;
    private String name;
    /** Se {@code false}, l'account è disattivato e non può effettuare login. */
    private boolean active = true;
    private LocalDateTime registeredAt;

    /** Numero di versione per lock ottimistico sul profilo. */
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    private Long version = 0L;

    /** Ruolo applicativo derivato dal tipo di sottoclasse JPA (WORKER, HOST, …). */
    @Transient
    public Role getRole() {
        if (this instanceof Worker) {
            return Role.WORKER;
        }
        if (this instanceof Host) {
            return Role.HOST;
        }
        if (this instanceof Technician) {
            return Role.TECHNICIAN;
        }
        if (this instanceof SysAdmin) {
            return Role.SYS_ADMIN;
        }
        throw new IllegalStateException("Ruolo non riconosciuto per l'utente: " + this.getClass().getName());
    }

    /** Traduce il flag active in uno stato leggibile (ATTIVO o SOSPESO). */
    @Transient
    public AccountStatus getStatus() {
        return active ? AccountStatus.ACTIVE : AccountStatus.SUSPENDED;
    }

    /** Si assicura che la versione non sia mai null dopo il caricamento. */
    @PostLoad
    private void ensureVersionForOptimisticLock() {
        if (version == null) {
            version = 0L;
        }
    }

    /** Prima di salvare per la prima volta, imposta la data di oggi e la versione a 0. */
    @PrePersist
    public void prePersist() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
        if (version == null) {
            version = 0L;
        }
    }
}
