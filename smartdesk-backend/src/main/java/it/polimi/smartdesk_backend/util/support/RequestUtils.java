package it.polimi.smartdesk_backend.util.support;

import jakarta.servlet.http.HttpServletRequest;

/** Utility per gestire dettagli delle richieste HTTP (es: IP reale dietro proxy). */
public class RequestUtils {

    /** Tenta di estrarre l'IP reale dall'header X-Forwarded-For (popolato da proxy/load balancer). Se assente, ripiega su getRemoteAddr(). */
    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Prende il primo IP della lista (quello originale del client)
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
