package it.polimi.smartdesk_backend.service.admin;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

/** Notifiche in-app agli sys admin per eventi che richiedono moderazione (nuovo host o spazio in coda di approvazione). */
@Service
@RequiredArgsConstructor
public class SysAdminNotificationService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Notifica tutti i sys admin di un host in attesa di approvazione; l'etichetta usa il nome commerciale o, in assenza, l'email. */
    public void notifyAdminsOfPendingHost(Host host) {
        String label = host.getName() != null && !host.getName().isBlank()
                ? host.getName()
                : host.getEmail();
        String msg = String.format(
                "Nuova richiesta di registrazione host da %s (%s). In attesa di approvazione.",
                label,
                host.getEmail());
        userRepository.findAllSysAdmins()
                .forEach(admin -> notificationService.sendUserNotification(admin.getId(), msg));
    }

    /** Notifica tutti i sys admin di uno spazio in attesa di approvazione, includendo nome spazio e identità host. */
    public void notifyAdminsOfPendingSpace(Space space, Host host) {
        String hostLabel = host.getName() != null && !host.getName().isBlank()
                ? host.getName()
                : host.getEmail();
        String msg = String.format(
                "Nuovo spazio \"%s\" da host %s (%s) in attesa di approvazione.",
                space.getName(),
                hostLabel,
                host.getEmail());
        userRepository.findAllSysAdmins()
                .forEach(admin -> notificationService.sendUserNotification(admin.getId(), msg));
    }
}

