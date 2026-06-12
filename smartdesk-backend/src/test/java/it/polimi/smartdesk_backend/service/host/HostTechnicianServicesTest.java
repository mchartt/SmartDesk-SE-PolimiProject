package it.polimi.smartdesk_backend.service.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianAssignedSpaceMapper;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import it.polimi.smartdesk_backend.service.desk.TechnicianDeskMaintenanceService;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.service.ticket.TechnicianAssignmentService;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class HostTechnicianServicesTest {

    private static final Long HOST_ID = 4L;

    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private HostRepository hostRepo;
    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private TechnicianMapper technicianMapper;
    @Mock
    private TechnicianAssignedSpaceMapper technicianAssignedSpaceMapper;
    @Mock
    private DeskRepository deskRepository;
    @Mock
    private DeskStateMachine deskStateMachine;
    @Mock
    private TechnicianAssignmentService technicianAssignmentService;

    private HostTechnicianDashboardService dashboardService;
    private HostTechnicianAccessService accessService;
    private TechnicianDeskMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        dashboardService = new HostTechnicianDashboardService(
                spaceRepo, hostRepo, technicianRepository, technicianMapper, technicianAssignedSpaceMapper);
        accessService = new HostTechnicianAccessService(spaceRepo);
        maintenanceService = new TechnicianDeskMaintenanceService(
                deskRepository, deskStateMachine, technicianAssignmentService);
    }

    @Test
    void shouldListTechniciansForHostDashboard() {
        Host host = EntityTestFixtures.host(HOST_ID, true);
        Space space = EntityTestFixtures.spaceMilano(10L, HOST_ID);
        Technician technician = new Technician();
        technician.setId(20L);
        technician.setSpaces(new HashSet<>(Set.of(space)));

        TechnicianDTO dto = new TechnicianDTO();
        dto.setTechnicianID(20L);
        TechnicianAssignedSpaceDTO assigned = new TechnicianAssignedSpaceDTO();
        assigned.setSpaceID(10L);

        when(hostRepo.findById(HOST_ID)).thenReturn(Optional.of(host));
        when(spaceRepo.findByHostID(HOST_ID)).thenReturn(List.of(space));
        when(technicianRepository.findForHostDashboard(HOST_ID)).thenReturn(List.of(technician));
        when(technicianMapper.toDto(technician)).thenReturn(dto);
        when(technicianAssignedSpaceMapper.toDto(space)).thenReturn(assigned);

        List<TechnicianDTO> result = dashboardService.getTechniciansForHost(HOST_ID);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getAssignedSpaces().size());
    }

    @Test
    void shouldRejectUnknownHostOnDashboard() {
        when(hostRepo.findById(HOST_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dashboardService.getTechniciansForHost(HOST_ID));
    }

    @Test
    void shouldAllowTechnicianCreatedByHost() {
        Technician technician = new Technician();
        technician.setId(20L);
        technician.setCreatingHostId(HOST_ID);
        technician.setSpaces(Set.of());

        accessService.assertTechnicianManagedByHost(HOST_ID, technician);
    }

    @Test
    void shouldAllowTechnicianLinkedToHostSpace() {
        Space space = EntityTestFixtures.spaceMilano(10L, HOST_ID);
        Technician technician = new Technician();
        technician.setId(20L);
        technician.setCreatingHostId(99L);
        technician.setSpaces(new HashSet<>(Set.of(space)));

        when(spaceRepo.findByHostID(HOST_ID)).thenReturn(List.of(space));

        accessService.assertTechnicianManagedByHost(HOST_ID, technician);
        assertTrue(accessService.technicianLinkedToSpace(technician, 10L));
    }

    @Test
    void shouldSetDeskMaintenanceForTechnician() {
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        when(deskRepository.lockByDeskIdForUpdate(12L)).thenReturn(Optional.of(desk));
        when(deskRepository.save(desk)).thenReturn(desk);

        maintenanceService.setDeskMaintenanceForTechnician(20L, 12L);

        verify(technicianAssignmentService).assertDeskAssignedToTechnician(20L, 12L);
        verify(deskStateMachine).markMaintenance(desk);
    }
}
