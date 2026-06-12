package it.polimi.smartdesk_backend.service.ticket;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianAssignedSpaceMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import it.polimi.smartdesk_backend.service.ticket.TechnicianAssignmentService;

/** Prova se riusciamo a dare i permessi ai tecnici sui vari spazi e controlla la dashboard. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class TechnicianAssignmentServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private DeskRepository deskRepository;

    @Spy
    private TechnicianAssignedSpaceMapper technicianAssignedSpaceMapper = new it.polimi.smartdesk_backend.mapper.TechnicianAssignedSpaceMapperImpl();

    @Mock
    private DeskStateMachine deskStateMachine;

    @InjectMocks
    private TechnicianAssignmentService technicianAssignmentService;

    @Test
    void assignedSpacesSorted() {
        Technician technician = new Technician();

        Space beta = new Space();
        beta.setSpaceID(2L);
        beta.setName("Beta Hub");
        beta.setOfficeCode("BBB222");

        Space alpha = new Space();
        alpha.setSpaceID(1L);
        alpha.setName("Alpha Desk");
        alpha.setOfficeCode("AAA111");

        technician.setSpaces(Set.of(beta, alpha));

        when(technicianRepository.findById(9L)).thenReturn(Optional.of(technician));

        List<TechnicianAssignedSpaceDTO> rows = technicianAssignmentService.listAssignedSpaces(9L);

        assertEquals(2, rows.size());
        assertEquals("Alpha Desk", rows.get(0).getName());
        assertEquals("Beta Hub", rows.get(1).getName());
        assertEquals("AAA111", rows.get(0).getOfficeCode());
    }

    @Test
    void spacesForUnknownTechnicianEmpty() {
        when(technicianRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(List.of(), technicianAssignmentService.listAssignedSpaces(99L));
    }

    @Test
    void listsDesksOnlyForAssignedSpace() {
        Technician technician = new Technician();
        Space space = new Space();
        space.setSpaceID(3L);
        space.setName("Milano Central");
        technician.setSpaces(Set.of(space));

        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setCode("A12");
        desk.setSpace(space);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        when(technicianRepository.findById(9L)).thenReturn(Optional.of(technician));
        when(deskRepository.findBySpaceSpaceID(3L)).thenReturn(List.of(desk));

        List<DeskDTO> desks = technicianAssignmentService.listAssignedDesks(9L, 3L);

        assertEquals(1, desks.size());
        assertEquals(12L, desks.get(0).getId());
        assertEquals("A12", desks.get(0).getCode());
    }

    @Test
    void blocksDeskOutsideAssignedSpaces() {
        Technician technician = new Technician();
        Space assigned = new Space();
        assigned.setSpaceID(1L);
        technician.setSpaces(Set.of(assigned));

        Space foreign = new Space();
        foreign.setSpaceID(2L);
        Desk desk = new Desk();
        desk.setDeskID(22L);
        desk.setSpace(foreign);
        desk.setStateCode(DeskStateCode.AVAILABLE);

        when(deskRepository.findById(22L)).thenReturn(Optional.of(desk));
        when(technicianRepository.findById(9L)).thenReturn(Optional.of(technician));

        assertThrows(NotFoundException.class,
                () -> technicianAssignmentService.assertDeskAssignedToTechnician(9L, 22L));
    }
}
