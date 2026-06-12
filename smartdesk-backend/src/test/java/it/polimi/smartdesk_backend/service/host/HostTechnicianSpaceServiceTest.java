package it.polimi.smartdesk_backend.service.host;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class HostTechnicianSpaceManagementServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private TechnicianMapper technicianMapper;

    @Mock
    private HostOwnershipService hostOwnershipService;

    @Mock
    private HostTechnicianAccessService hostTechnicianAccessService;

    @InjectMocks
    private HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;

    @Test
    void blocksDuplicateAssignment() {
        Space space = new Space();
        space.setSpaceID(10L);
        Technician technician = new Technician();

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(4L, 10L)).thenReturn(space);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));
        when(hostTechnicianAccessService.technicianLinkedToSpace(technician, 10L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> hostTechnicianSpaceManagementService.assignTechnicianToSpace(4L, 10L, 22L));
    }

    @Test
    void unassignFailsIfTechnicianWasNotInSpace() {
        Space space = new Space();
        space.setSpaceID(10L);
        Technician technician = new Technician();

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(4L, 10L)).thenReturn(space);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));
        when(hostTechnicianAccessService.technicianLinkedToSpace(technician, 10L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> hostTechnicianSpaceManagementService.unassignTechnicianFromSpace(4L, 10L, 22L));
    }

    @Test
    void ensureLinksManagedTechnicianToSpaceTicket() {
        Space space = new Space();
        space.setSpaceID(10L);
        Technician technician = new Technician();

        when(hostOwnershipService.loadOwnedSpaceOrNotFound(4L, 10L)).thenReturn(space);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));
        when(hostTechnicianAccessService.technicianLinkedToSpace(technician, 10L)).thenReturn(false);

        hostTechnicianSpaceManagementService.ensureTechnicianLinkedToSpace(4L, 10L, 22L);

        verify(hostTechnicianAccessService).assertTechnicianManagedByHost(4L, technician);
        verify(technicianRepository).save(technician);
    }
}
