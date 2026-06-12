package it.polimi.smartdesk_backend.dto.common;

import it.polimi.smartdesk_backend.model.user.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Claim estratti dal JWT access prima di costruire {@link AuthenticatedUser}. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenClaims {
    private Long userId;
    private Role role;
    private long issuedAtEpochSeconds;
    private long expiresAtEpochSeconds;
    private String tokenId;
}
