package it.polimi.smartdesk_backend.service.review;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import it.polimi.smartdesk_backend.model.review.Review;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;

/** Notifiche push/email di attività legate alle recensioni (nuova recensione, risposta host). */
@Service
@RequiredArgsConstructor
public class ReviewNotificationService {

    public static final String HOST_REVIEW_LEFT = "HOST_REVIEW_LEFT";

    private final NotificationService notificationService;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;

    /** Avvisa l'host che un worker ha lasciato una recensione su uno dei suoi spazi. */
    public void notifyHostOfNewReview(Review review, Space space, Long workerId) {
        String label = space.getName() != null && !space.getName().isBlank()
                ? space.getName().trim()
                : "un tuo spazio";
        User worker = userRepo.findById(workerId).orElse(null);
        notificationService.sendWorkerActivityNotification(
                review.getHostID(),
                "Nuova recensione per «" + label + "». Apri Recensioni per leggerla.",
                HOST_REVIEW_LEFT,
                userField(worker, User::getName),
                userField(worker, User::getSurname),
                userField(worker, User::getEmail),
                review.getRating());
    }



    private static String shortSnippet(String comment) {
        String snippet = comment == null ? "" : comment;
        return snippet.length() > 50 ? snippet.substring(0, 47) + "..." : snippet;
    }

    private static String userField(User user, Function<User, String> getter) {
        return userField(user, getter, "");
    }

    private static String userField(User user, Function<User, String> getter, String fallback) {
        if (user == null) {
            return fallback;
        }
        String value = getter.apply(user);
        return value == null ? fallback : value;
    }
}

