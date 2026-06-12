package it.polimi.smartdesk_backend.service.space;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.mapper.SpaceMapper;
import it.polimi.smartdesk_backend.dto.space.SpaceRequestDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.review.ReviewRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;
import it.polimi.smartdesk_backend.service.admin.SysAdminNotificationService;
import it.polimi.smartdesk_backend.service.admin.AdminSpaceService;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;

/** Casi limite e anomalie su {@link SpaceManagementService} (host mancante, stati incoerenti). Rimosse validazioni orari over-engineered per scopi didattici. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class SpaceManagementServiceOdditiesTest {

    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private HostRepository hostRepo;
    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private SysAdminNotificationService sysAdminNotificationService;
    @Mock
    private ReviewStatsService reviewStatsService;
    @Mock
    private HostOwnershipService hostOwnershipService;
    @Spy
    private OpeningHoursService openingHoursService = new OpeningHoursService(new ObjectMapper());
    @Mock
    private SpaceMapper spaceMapper;

    @InjectMocks
    private SpaceManagementService SpaceManagementService;

    private AdminSpaceService adminSpaceService;

    @BeforeEach
    void setUp() {
        adminSpaceService = new AdminSpaceService(
                spaceRepo, reviewStatsService, hostOwnershipService, spaceMapper, SpaceManagementService);
        lenient().when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of());
        lenient().when(spaceMapper.toDto(any(Space.class))).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));
    }

    @Test
    void findByIdPendingAsNotFound() {
        Space s = EntityTestFixtures.spaceMilano(3L, 2L);
        s.setApproved(false);
        when(spaceRepo.findById(3L)).thenReturn(Optional.of(s));
        assertThrows(NotFoundException.class, () -> SpaceManagementService.findById(3L));
    }

    @Test
    void hostNameMissingDoesNotExplode() {
        Space s = EntityTestFixtures.spaceMilano(2L, 99L);
        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(s));
        lenient().when(hostRepo.findById(99L)).thenReturn(Optional.empty());
        assertNull(SpaceManagementService.findAll().get(0).getHostName());
    }

    @Test
    void adminListIncludesPending() {
        Space ok = EntityTestFixtures.spaceMilano(1L, 5L);
        Space pending = EntityTestFixtures.spaceMilano(2L, 5L);
        pending.setApproved(false);
        when(spaceRepo.findAll()).thenReturn(List.of(ok, pending));
        lenient().when(hostRepo.findById(5L)).thenReturn(Optional.empty());
        assertEquals(2, adminSpaceService.findAllForAdmin().size());
    }

    @Test
    void deleteSpaceForWrongHost() {
        Space s = EntityTestFixtures.spaceMilano(5L, 2L);
        lenient().when(spaceRepo.findById(5L)).thenReturn(Optional.of(s));
        doThrow(new NotFoundException("not found"))
                .when(hostOwnershipService).loadOwnedSpaceOrNotFound(99L, 5L);
        assertThrows(NotFoundException.class, () -> SpaceManagementService.deleteSpaceForHost(99L, 5L));
    }

    @Test
    void newSpacePingAdmin() {
        Host host = EntityTestFixtures.host(2L, true);
        SpaceRequestDTO body = new SpaceRequestDTO();
        body.setName("Nuovo");
        body.setAddress("Via 1");
        body.setCity("Milano");
        body.setDescription("Desc");
        when(hostOwnershipService.loadHostOrNotFound(2L)).thenReturn(host);

        when(spaceRepo.save(any(Space.class))).thenAnswer(inv -> {
            Space saved = inv.getArgument(0);
            saved.setSpaceID(9L);
            return saved;
        });
        SpaceManagementService.createSpace(2L, body);
        verify(sysAdminNotificationService).notifyAdminsOfPendingSpace(any(Space.class), eq(host));
    }

    private static SpaceDTO dtoFrom(Space space) {
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(space.getSpaceID());
        dto.setName(space.getName());
        dto.setCity(space.getCity());
        dto.setApproved(space.isApproved());
        dto.setOfficeCode(space.getOfficeCode());
        dto.setDeskCount(space.getDesks().size());
        return dto;
    }
}
