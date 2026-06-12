package it.polimi.smartdesk_backend.dto.space;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Spazio coworking in risposta API: anagrafica, orari, flag approvazione e media recensioni ({@code null} se assente). */
@Data
public class SpaceDTO {

    /** Identificativo interno spazio. */
    private Long spaceID;

    @Size(max = 50, message = "Il nome non può superare i 50 caratteri.")
    private String name;

    @Size(max = 100, message = "L'indirizzo non può superare i 100 caratteri.")
    private String address;

    @Size(max = 50, message = "La città non può superare i 50 caratteri.")
    private String city;

    @Size(max = 500, message = "La descrizione non può superare i 500 caratteri.")
    private String description;

    /** Orari settimanali per giorno (chiavi: {@code MONDAY}, …, {@code SUNDAY}); ordine JSON stabile con {@link LinkedHashMap}. */
    private Map<String, OpeningHoursDayDTO> openingHours;

    /** Nome display del proprietario host. */
    private String hostName;

    /** Nome host (lista admin / arricchimenti). */
    private String hostGivenName;
    /** Cognome host (lista admin / arricchimenti). */
    private String hostFamilyName;
    /** Email host. */
    private String hostEmail;

    /** Partita IVA (ovviamente solo per gli host). */
    private String hostVatNumber;

    /** Media voti recensioni per questo spazio; {@code null} se assenti. */
    private Double averageReviewRating;

    /** Indica se lo spazio è approvato e prenotabile. */
    private boolean approved;
    /** Numero postazioni nello spazio (denormalizzato per liste). */
    private int deskCount;

    /** Codice ufficio opaco a 6 caratteri alfanumerici (supporto/UX). */
    private String officeCode;

}

