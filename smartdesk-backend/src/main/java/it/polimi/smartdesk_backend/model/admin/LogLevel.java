package it.polimi.smartdesk_backend.model.admin;

/** Livello evento in {@link SystemLog}; AUDIT per azioni tracciabili da console admin. */
public enum LogLevel {
    INFO,
    WARN,
    ERROR,
    AUDIT
}

