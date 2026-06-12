package it.polimi.smartdesk_backend.model.user;

/** Ciclo vita account: PENDING_APPROVAL blocca operatività host fino ad approvazione SYS_ADMIN. */
public enum AccountStatus {
    PENDING_APPROVAL,
    ACTIVE,
    SUSPENDED
}

