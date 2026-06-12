package it.polimi.smartdesk_backend.model.space;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/** Preset amenity riusabile (nome, hint, elenco ordinato) per desk dello stesso {@link Space}. */
@Entity
@Table(name = "space_amenity_presets")
@Getter
@Setter
@NoArgsConstructor
public class SpaceAmenityPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long presetID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(nullable = false, length = 48)
    private String label;

    /** Nota opzionale breve mostrata accanto al nome nella UI. */
    @Column(length = 120)
    private String hint;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "space_amenity_preset_items", joinColumns = @JoinColumn(name = "preset_id"))
    @Column(name = "amenity", nullable = false, length = 12)
    @OrderColumn(name = "sort_ord")
    private List<String> amenities = new ArrayList<>();

    /** Sostituisce gli elementi copiando la lista (null → lista vuota). */
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }
}

