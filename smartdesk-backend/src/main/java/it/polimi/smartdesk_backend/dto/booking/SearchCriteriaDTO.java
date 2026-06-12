package it.polimi.smartdesk_backend.dto.booking;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

/** Criteri di ricerca postazioni per worker (giorno target, fascia oraria, filtri opzionali). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDTO {

    /** Giorno di riferimento per la ricerca disponibilità. */
    @NotNull(message = "La data di ricerca è obbligatoria.")
    private LocalDate targetDate;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private List<String> requiredAmenities = new ArrayList<>();
    private String city;
    /** Filtro per spazio specifico. */
    private Long spaceId;

    /** Se {@code true}, include postazioni in manutenzione come non prenotabili. */
    private Boolean includeMaintenance = Boolean.FALSE;

    /** Copia difensiva dei servizi obbligatori richiesti in ricerca. */
    public List<String> getRequiredAmenities() {
        return requiredAmenities == null ? new ArrayList<>() : new ArrayList<>(requiredAmenities);
    }

    /** Salva una copia mutabile; {@code null} → lista vuota. */
    public void setRequiredAmenities(List<String> requiredAmenities) {
        this.requiredAmenities = requiredAmenities == null ? new ArrayList<>() : new ArrayList<>(requiredAmenities);
    }

}

