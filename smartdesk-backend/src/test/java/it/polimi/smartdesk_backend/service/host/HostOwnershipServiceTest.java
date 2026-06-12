package it.polimi.smartdesk_backend.service.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class HostOwnershipServiceTest {

    private static final Long HOST_ID = 4L;
    private static final Long OTHER_HOST_ID = 99L;
    private static final Long SPACE_ID = 10L;
    private static final Long DESK_ID = 20L;

    @Mock
    private HostRepository hostRepo;
    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private DeskRepository deskRepo;

    private HostOwnershipService hostOwnershipService;

    @BeforeEach
    void setUp() {
        hostOwnershipService = new HostOwnershipService(hostRepo, spaceRepo, deskRepo);
    }

    @Test
    void shouldLoadOwnedSpace() {
        Host host = EntityTestFixtures.host(HOST_ID, true);
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);

        when(hostRepo.findById(HOST_ID)).thenReturn(Optional.of(host));
        when(spaceRepo.findById(SPACE_ID)).thenReturn(Optional.of(space));

        Space loaded = hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID);

        assertEquals(SPACE_ID, loaded.getSpaceID());
    }

    @Test
    void shouldRejectSpaceOwnedByAnotherHost() {
        Host host = EntityTestFixtures.host(HOST_ID, true);
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, OTHER_HOST_ID);

        when(hostRepo.findById(HOST_ID)).thenReturn(Optional.of(host));
        when(spaceRepo.findById(SPACE_ID)).thenReturn(Optional.of(space));

        assertThrows(NotFoundException.class,
                () -> hostOwnershipService.loadOwnedSpaceOrNotFound(HOST_ID, SPACE_ID));
    }

    @Test
    void shouldRejectDeskOwnedByAnotherHost() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, OTHER_HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(DESK_ID);
        desk.setSpace(space);

        when(deskRepo.findById(DESK_ID)).thenReturn(Optional.of(desk));

        assertThrows(NotFoundException.class,
                () -> hostOwnershipService.assertDeskOwnedByHostOrNotFound(HOST_ID, DESK_ID));
    }

    @Test
    void shouldFindHostsByIds() {
        Host host = EntityTestFixtures.host(HOST_ID, true);
        when(hostRepo.findAllById(List.of(HOST_ID))).thenReturn(List.of(host));

        var map = hostOwnershipService.findAllByIds(List.of(HOST_ID));

        assertEquals(1, map.size());
        assertEquals(host, map.get(HOST_ID));
    }
}
