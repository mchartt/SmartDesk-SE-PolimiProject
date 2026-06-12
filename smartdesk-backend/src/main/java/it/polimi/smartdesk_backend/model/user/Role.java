package it.polimi.smartdesk_backend.model.user;

/** Ruolo applicativo (discriminator {@code app_user.role_type} e claim JWT). */
public enum Role {
    WORKER,
    HOST,
    TECHNICIAN,
    SYS_ADMIN
}

