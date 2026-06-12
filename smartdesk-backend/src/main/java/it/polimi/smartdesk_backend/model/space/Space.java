package it.polimi.smartdesk_backend.model.space;
import it.polimi.smartdesk_backend.model.user.Technician;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/** Coworking: anagrafica, {@code approved}, orari JSON, stanze/desk e tecnici assegnati (many-to-many). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spaceID;

    private String description;
    private Long hostID;
    private String name;
    private String address;
    private String city;

    /** Codice univoco di 6 caratteri (es. MIL001) usato per identificare l'ufficio nelle stampe o in UI. */
    @Column(name = "office_code", length = 6, unique = true)
    private String officeCode;

    /** Se false, l'ufficio è ancora in bozza e non può essere prenotato dai worker. */
    private boolean approved = false;

    /** Gli orari di apertura salvati in formato JSON (da Lunedì a Domenica). */
    @Column(name = "opening_hours_json", columnDefinition = "TEXT")
    private String openingHoursJson;

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Desk> desks = new ArrayList<>();

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceAmenityPreset> amenityPresets = new ArrayList<>();

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceClosure> closures = new ArrayList<>();

    @ManyToMany(mappedBy = "spaces")
    private Set<Technician> technicians = new HashSet<>();

    /** Aggiunge un desk a questo ufficio. */
    public void addDesk(Desk desk) {
        desk.setSpace(this);
        desks.add(desk);
    }

    /** Rimuove un desk dall'ufficio. */
    public void removeDesk(Desk desk) {
        desks.remove(desk);
        desk.setSpace(null);
    }

    /** Aggiunge una stanza all'ufficio. */
    public void addRoom(Room room) {
        room.setSpace(this);
        rooms.add(room);
    }

    /** Aggiunge un set predefinito di dotazioni per i desk di questo ufficio. */
    public void addAmenityPreset(SpaceAmenityPreset preset) {
        amenityPresets.add(preset);
        preset.setSpace(this);
    }

    /** Rimuove un set predefinito di dotazioni. */
    public void removeAmenityPreset(SpaceAmenityPreset preset) {
        amenityPresets.remove(preset);
        preset.setSpace(null);
    }

    /** Controlla se due uffici sono lo stesso basandosi sull'ID del database. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Space other)) {
            return false;
        }
        return spaceID != null && spaceID.equals(other.spaceID);
    }

    /** Genera un codice identificativo basato sull'ID per le mappe di memoria. */
    @Override
    public int hashCode() {
        return spaceID == null ? 0 : spaceID.hashCode();
    }

}

