package it.polimi.smartdesk_backend.mapper;

import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.User;

/** Converte l'utente nel riassunto profilo da mandare al client, togliendo la roba privata come la password. */
public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    /** Crea l'oggetto utente da mandare al client, togliendo la roba privata. */
    public static UserProfileDTO fromUser(User user) {
        UserProfileDTO dto = baseFrom(user);
        dto.setApproved(user instanceof Host host ? host.isApproved() : true);
        return dto;
    }

    /** Come {@link #fromUser} ma aggiunge description e nameStructure per la vista admin. */
    public static UserProfileDTO adminFromUser(User user) {
        UserProfileDTO dto = fromUser(user);
        if (user instanceof Host host) {
            dto.setDescription(host.getDescription());
            dto.setNameStructure(host.getNameStructure());
        }
        return dto;
    }

    private static UserProfileDTO baseFrom(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserID(user.getId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setRegisteredAt(user.getRegisteredAt());
        return dto;
    }
}
