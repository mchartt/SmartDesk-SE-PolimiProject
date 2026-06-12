package it.polimi.smartdesk_backend.model.user;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Refresh token opaco collegato a {@link User}: scadenza, revoca e rotazione su login/logout/cambio password. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenID;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    /** {@code true} se l'istante corrente è oltre {@link #expiryDate}. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}

