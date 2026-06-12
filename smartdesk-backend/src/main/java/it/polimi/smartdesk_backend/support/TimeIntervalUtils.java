package it.polimi.smartdesk_backend.support;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Confronto intervalli half-open style per le prenotazioni: due fasce si sovrappongono se ciascuna inizia prima che l’altra finisca. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeIntervalUtils {

    /**
     * @return {@code false} se un estremo è {@code null}; altrimenti overlap classico {@code [start, end)}
     */
    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
            LocalDateTime bStart, LocalDateTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            return false;
        }
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}

