package it.polimi.smartdesk_backend.dto.space;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload creazione/aggiornamento postazione: spazio, stanza, codice e lista servizi opzionale. Il getter {@link #getAmenities()} ritorna sempre una copia mutabile. */
@Data
public class DeskRequestDTO {

    private Long spaceID;

    @Size(max = 30, message = "Il codice postazione non può superare i 30 caratteri.")
    private String code;

    private Long roomID;

    private Double pricePerHour;

    private List<String> amenities = new ArrayList<>();

    /** {@inheritDoc} */
    public List<String> getAmenities() {
        return amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }

    /** {@inheritDoc} */
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }
}

