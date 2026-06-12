package it.polimi.smartdesk_backend.service.ticket;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;

import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.ticket.state.TicketStateMachine;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class TicketServiceQueryTest {

    @Mock
    private TicketRepository ticketRepo;

    @Mock
    private TicketResponseService ticketResponseService;

    @Mock
    private HostOwnershipService hostOwnershipService;

    @Mock
    private DeskRepository deskRepo;

    @Mock
    private SpaceRepository spaceRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;

    @Mock
    private TicketStateMachine ticketStateMachine;

    @Mock
    private TicketTechnicianNoteRepository ticketTechnicianNoteRepo;

    @Mock
    private TicketHostNoteRepository ticketHostNoteRepo;

    @Mock
    private TicketCreationService ticketCreationService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void pendingTicketsAreScopedToTechnicianAssignedSpaces() {
        ticketService.getPendingTicketResponses(7L);

        verify(ticketRepo).findOpenTicketsInSpacesAssignedToTechnician(7L);
    }
}
