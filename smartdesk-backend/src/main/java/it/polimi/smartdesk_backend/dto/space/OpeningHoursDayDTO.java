package it.polimi.smartdesk_backend.dto.space;

import lombok.Data;

/** Fascia oraria giornaliera per uno spazio (una voce della mappa {@code openingHours} in {@link SpaceDTO}). */
@Data
public class OpeningHoursDayDTO {

    /** Se {@code true}, lo spazio è chiuso; {@code open}/{@code close} ignorati. */
    private boolean closed = true;

    /** Apertura locale {@code HH:mm}. */
    private String open;

    /** Chiusura locale {@code HH:mm}. */
    private String close;
}

