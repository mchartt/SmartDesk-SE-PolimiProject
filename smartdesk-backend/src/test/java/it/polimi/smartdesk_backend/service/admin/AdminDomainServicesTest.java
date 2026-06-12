package it.polimi.smartdesk_backend.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.mapper.SpaceMapper;
import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.service.review.ReviewStatsService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class AdminDomainServicesTest {

    private static final Long HOST_ID = 4L;
    private static final Long SPACE_ID = 10L;
    private static final Long ADMIN_ID = 1L;

    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private ReviewStatsService reviewStatsService;
    @Mock
    private HostOwnershipService hostOwnershipService;
    @Mock
    private SpaceMapper spaceMapper;
    @Mock
    private SpaceManagementService spaceManagementService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private AdminSpaceService adminSpaceService;
    private SysAdminNotificationService sysAdminNotificationService;

    @BeforeEach
    void setUp() {
        adminSpaceService = new AdminSpaceService(
                spaceRepo, reviewStatsService, hostOwnershipService, spaceMapper, spaceManagementService);
        sysAdminNotificationService = new SysAdminNotificationService(userRepository, notificationService);
    }

  // --- AdminSpaceService ---

    @Test
    void shouldListAllSpacesForAdmin() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(SPACE_ID);

        when(spaceRepo.findAll()).thenReturn(List.of(space));
        when(spaceMapper.toDto(space)).thenReturn(dto);
        when(spaceManagementService.enrich(eq(space), eq(dto), eq(null), eq(null))).thenReturn(dto);

        List<SpaceDTO> result = adminSpaceService.findAllForAdmin();

        assertEquals(1, result.size());
        assertEquals(SPACE_ID, result.get(0).getSpaceID());
    }

    @Test
    void shouldListApprovedSpacesEnrichedForAdmin() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        space.setCity("Milano");
        space.setName("Hub");
        Host host = EntityTestFixtures.host(HOST_ID, true);
        host.setEmail("host@test.it");
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(SPACE_ID);

        when(reviewStatsService.averageRatingBySpaceId()).thenReturn(Map.of(SPACE_ID, 4.5));
        when(spaceRepo.findByApprovedTrue()).thenReturn(List.of(space));
        when(hostOwnershipService.findAllByIds(Set.of(HOST_ID))).thenReturn(Map.of(HOST_ID, host));
        when(spaceMapper.toDto(space)).thenReturn(dto);
        when(spaceManagementService.enrich(space, dto, 4.5, host)).thenReturn(dto);

        List<SpaceDTO> result = adminSpaceService.findApprovedEnrichedForAdmin();

        assertEquals(1, result.size());
    }

    @Test
    void shouldListPendingSpacesForAdmin() {
        Space pending = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        pending.setApproved(false);
        SpaceDTO dto = new SpaceDTO();

        when(spaceRepo.findByApprovedFalse()).thenReturn(List.of(pending));
        when(spaceMapper.toDto(pending)).thenReturn(dto);
        when(spaceManagementService.enrich(pending, dto, null, null)).thenReturn(dto);

        List<SpaceDTO> result = adminSpaceService.findPendingApprovalForAdmin();

        assertEquals(1, result.size());
    }

  // --- SysAdminNotificationService ---

    @Test
    void shouldNotifyAllAdminsOfPendingHost() {
        Host host = EntityTestFixtures.host(HOST_ID, false);
        host.setName("Coworking SRL");
        host.setEmail("host@example.com");
        SysAdmin admin = EntityTestFixtures.admin(ADMIN_ID);

        when(userRepository.findAllSysAdmins()).thenReturn(List.of(admin));

        sysAdminNotificationService.notifyAdminsOfPendingHost(host);

        verify(notificationService).sendUserNotification(
                eq(ADMIN_ID),
                eq("Nuova richiesta di registrazione host da Coworking SRL (host@example.com). In attesa di approvazione."));
    }

    @Test
    void shouldNotifyAllAdminsOfPendingSpace() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        space.setName("Nuovo Hub");
        Host host = EntityTestFixtures.host(HOST_ID, true);
        host.setName("Luigi");
        host.setEmail("luigi@test.it");
        SysAdmin admin = EntityTestFixtures.admin(ADMIN_ID);

        when(userRepository.findAllSysAdmins()).thenReturn(List.of(admin));

        sysAdminNotificationService.notifyAdminsOfPendingSpace(space, host);

        verify(notificationService).sendUserNotification(
                eq(ADMIN_ID),
                eq("Nuovo spazio \"Nuovo Hub\" da host Luigi (luigi@test.it) in attesa di approvazione."));
    }

    @Test
    void shouldUseEmailWhenHostNameMissing() {
        Host host = EntityTestFixtures.host(HOST_ID, false);
        host.setName("");
        host.setEmail("only@mail.it");
        SysAdmin admin = EntityTestFixtures.admin(ADMIN_ID);

        when(userRepository.findAllSysAdmins()).thenReturn(List.of(admin));

        sysAdminNotificationService.notifyAdminsOfPendingHost(host);

        verify(notificationService).sendUserNotification(
                ADMIN_ID,
                "Nuova richiesta di registrazione host da only@mail.it (only@mail.it). In attesa di approvazione.");
        assertTrue(host.getEmail().contains("@"));
    }
}
