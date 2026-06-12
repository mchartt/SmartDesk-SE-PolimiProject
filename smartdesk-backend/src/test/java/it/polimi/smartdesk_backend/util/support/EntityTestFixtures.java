package it.polimi.smartdesk_backend.util.support;

import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.model.user.Role;
import lombok.experimental.UtilityClass;
import org.springframework.test.util.ReflectionTestUtils;

/** Entita' minime per i test: solo campi davvero necessari al caso. */
@UtilityClass
public class EntityTestFixtures {

    public static Worker worker(long id) {
        Worker w = new Worker();
        return active(w, id);
    }

    public static Worker workerBanned(long id) {
        Worker w = worker(id);
        w.setActive(false);
        return w;
    }

    public static Host host(long id, boolean approved) {
        Host h = new Host();
        h.setApproved(approved);
        return active(h, id);
    }

    public static Space spaceMilano(long spaceId, long hostId) {
        Space s = new Space();
        s.setSpaceID(spaceId);
        s.setHostID(hostId);
        s.setName("Milano Hub");
        s.setCity("Milano");
        s.setAddress("Via Test");
        s.setDescription("Centro coworking");
        s.setApproved(true);
        return s;
    }

    public static SysAdmin admin(long id) {
        SysAdmin a = new SysAdmin();
        return active(a, id);
    }

    public static AuthenticatedUser principal(long userId, Role role) {
        return new AuthenticatedUser(userId, role);
    }

    private static <T extends User> T active(T user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        user.setActive(true);
        return user;
    }
}

