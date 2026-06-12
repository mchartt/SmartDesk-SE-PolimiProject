package it.polimi.smartdesk_backend.dto.common;

import it.polimi.smartdesk_backend.model.user.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Principal Spring Security dopo JWT: {@code userId} + {@link it.polimi.smartdesk_backend.model.user.Role}. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatedUser {
    private Long userId;
    private Role role;
}
