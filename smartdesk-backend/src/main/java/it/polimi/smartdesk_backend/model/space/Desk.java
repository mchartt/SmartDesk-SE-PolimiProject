package it.polimi.smartdesk_backend.model.space;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Postazione in uno {@link Space}; {@code code} univoco per spazio. {@code stateCode} = AVAILABLE/MAINTENANCE/RESERVED; l'occupazione per fascia oraria è sulle prenotazioni, non su BOOKED persistito. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_desk_space_code", columnNames = { "space_id", "code" }))
@Getter
@Setter
@NoArgsConstructor
public class Desk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deskID;

    private String code;

    private String building;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "desk_amenities", joinColumns = @JoinColumn(name = "desk_id"))
    private List<String> amenities = new ArrayList<>();

    @Convert(converter = DeskStateCodeConverter.class)
    private DeskStateCode stateCode = DeskStateCode.AVAILABLE;

    @Convert(converter = DeskStateCodeConverter.class)
    private DeskStateCode previousStateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private double pricePerHour;

    /** {@code null} → lista vuota (non persiste null in collection). */
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? new ArrayList<>() : new ArrayList<>(amenities);
    }
}

