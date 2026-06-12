package it.polimi.smartdesk_backend.model.admin;
import it.polimi.smartdesk_backend.model.user.User;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Utente con ruolo SYS_ADMIN (discriminator JPA). */
@Entity
@DiscriminatorValue("SYS_ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class SysAdmin extends User {
}

