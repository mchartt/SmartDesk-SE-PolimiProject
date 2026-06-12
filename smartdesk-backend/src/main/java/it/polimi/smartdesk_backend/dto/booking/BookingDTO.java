package it.polimi.smartdesk_backend.dto.booking;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Quello che vedi quando chiedi i dettagli di una prenotazione. È un po' un riassunto di tutto: chi ha prenotato, quale desk ha preso, in che ufficio si trova e quando. Lo usiamo sia per i worker che per gli admin, arricchendo i dati in base a chi guarda. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {

    /** Identificativo interno della prenotazione. */
    private Long bookingID;
    /** Codice pubblico a 6 caratteri (A–Z, 0–9); {@code null} solo su dati legacy non ancora migrati. */
    private String bookingCode;
    /** Identificativo interno postazione. */
    private Long deskID;
    /** Codice postazione nello spazio. */
    private String deskCode;
    /** Nome dello spazio (coworking). */
    private String spaceName;
    /** Città della sede, se nota. */
    private String city;
    /** Edificio o alias edificio mostrato in lista. */
    private String buildingName;
    /** Identificativo worker titolare. */
    private Long workerID;
    /** Email worker; valorizzata principalmente sulle liste admin. */
    private String workerEmail;
    /** Nome completo o display worker; valorizzata principalmente sulle liste admin. */
    private String workerName;
    /** Giorno di calendario della prenotazione (allineato a regole ticket e UI worker). */
    private LocalDate bookedDay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** Stato dominio della prenotazione (stringa serializzata). */
    private String status;
    /** Versione ottimistica della prenotazione, da rimandare quando la si modifica. */
    private Long version;

    /** Costruttore ridotto con id desk e fascia oraria. */
    public BookingDTO(Long bookingID, Long deskID, LocalDateTime startTime, LocalDateTime endTime) {
        this.bookingID = bookingID;
        this.deskID = deskID;
        this.startTime = startTime;
        this.endTime = endTime;
    }

}

