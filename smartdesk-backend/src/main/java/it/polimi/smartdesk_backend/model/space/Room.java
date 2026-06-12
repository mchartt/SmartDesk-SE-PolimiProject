package it.polimi.smartdesk_backend.model.space;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/** Stanza dentro un ufficio — il code non si ripete nello stesso space. */
@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_space_code", columnNames = { "space_id",
        "code" }))
@Getter
@Setter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(nullable = false, length = 80)
    private String name;

    /** Codice stanza (es. TA, SR), univoco nello spazio. */
    @Column(nullable = false, length = 10)
    private String code;
}

