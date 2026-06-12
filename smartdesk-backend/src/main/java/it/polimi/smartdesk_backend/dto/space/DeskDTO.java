package it.polimi.smartdesk_backend.dto.space;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Vista desk per catalogo/ricerca worker: codice, stanza, amenities, stato operativo e flag prenotabilità UI. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeskDTO {

    private Long id;

    @Size(max = 30, message = "Il codice postazione non può superare i 30 caratteri.")
    private String code;

    @Size(max = 50, message = "Edificio o etichetta luogo: massimo 50 caratteri.")
    private String building;

    private Long roomID;
    private String roomName;
    private String roomCode;

    private List<String> amenities = new ArrayList<>();
    private String currentState;
    private Long spaceID;
    /** {@code null} se lo spazio non ha ancora recensioni. */
    private Double spaceAverageRating;
    private double pricePerHour;
    /** {@code false} → visibile in catalogo ma bloccata (es. manutenzione). Default {@code true}. */
    private Boolean bookable = Boolean.TRUE;
    /** Costruttore base per catalogo (id, codice, edificio, servizi). */
    public DeskDTO(Long id, String code, String building, List<String> amenities) {
        this.id = id;
        this.code = code;
        this.building = building;
        this.amenities = amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }

    /** Copia difensiva della lista servizi. */
    public List<String> getAmenities() {
        return amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }

    /** Salva una copia mutabile; {@code null} → lista vuota. */
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }

}

