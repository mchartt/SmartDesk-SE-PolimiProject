package it.polimi.smartdesk_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Stato di una fascia oraria per una postazione in un giorno (etichetta ora + stato disponibilità). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotStatusDTO {
    /** Etichetta orario (formato dipendente dal servizio). */
    private String time;
    /** Stato slot (es. libero, occupato, … stringa dominio). */
    private String status;
}

