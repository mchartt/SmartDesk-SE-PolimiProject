package it.polimi.smartdesk_backend.service.ticket;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.service.ticket.state.TicketStateMachine;
import it.polimi.smartdesk_backend.util.message.TicketMessage;

import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class TicketServiceAssignmentTest {

    @Mock
    private TicketRepository ticketRepo;

    @Mock
    private TicketTechnicianNoteRepository ticketTechnicianNoteRepo;

    @Mock
    private TicketHostNoteRepository ticketHostNoteRepo;

    @Mock
    private DeskRepository deskRepo;

    @Mock
    private SpaceRepository spaceRepo;

    @Mock
    private HostOwnershipService hostOwnershipService;

    @Mock
    private HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TicketResponseService ticketResponseService;

    @Mock
    private TicketStateMachine ticketStateMachine;

    @Mock
    private TicketCreationService ticketCreationService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void assignsTechnicianAndLinksSpaceWhenMissingOnTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setTicketCode("T001");
        ticket.setDeskID(12L);
        ticket.setWorkerID(5L);
        ticket.setStatus(TicketStatus.OPEN.name());
        Space space = new Space();
        space.setSpaceID(9L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(77L);

        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));
        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);
        
        // Simula comportamento state machine
        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setTechnicianID(invocation.getArgument(1));
            t.setStatus(TicketStatus.IN_PROGRESS.name());
            return null;
        }).when(ticketStateMachine).assignTechnician(any(), anyLong());

        TicketResponseDTO response = ticketService.assignTechnicianToTicket(4L, 77L, 3L, "HIGH");

        assertEquals(77L, response.getTicketID());
        assertEquals(9L, ticket.getSpaceID());
        assertEquals(3L, ticket.getTechnicianID());
        assertEquals("HIGH", ticket.getSeverity());
        assertEquals(TicketStatus.IN_PROGRESS.name(), ticket.getStatus());
        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(4L, 12L);
        verify(hostTechnicianSpaceManagementService).ensureTechnicianLinkedToSpace(4L, 9L, 3L);
        verify(notificationService).notifyTicketAssigned(5L, "T001");
        verify(notificationService).notifyTicketAssignedToTechnician(3L, "T001");
    }

    @Test
    void rejectsTicketNotOpen() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setDeskID(12L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        Space space = new Space();
        space.setSpaceID(9L);
        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setSpace(space);

        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));
        when(deskRepo.findById(12L)).thenReturn(Optional.of(desk));
        
        // Simula errore della state machine per transazione non valida
        doThrow(new BusinessRuleException(TicketMessage.TICKET_ASSIGN_ONLY_WHEN_OPEN.text()))
                .when(ticketStateMachine).assignTechnician(any(), anyLong());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ticketService.assignTechnicianToTicket(4L, 77L, 3L, "HIGH"));

        assertEquals(TicketMessage.TICKET_ASSIGN_ONLY_WHEN_OPEN.text(), ex.getMessage());
    }

    @Test
    void notifiesOnlyWorkerWhenTechnicianChangesNote() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setTicketCode("T001");
        ticket.setSpaceID(9L);
        ticket.setDeskID(12L);
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(3L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        ticket.setTechnicianNote("Vecchia nota");
        Space space = new Space();
        space.setSpaceID(9L);
        space.setHostID(4L);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(77L);

        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));
        lenient().when(spaceRepo.findById(9L)).thenReturn(Optional.of(space));
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO response = ticketService.updateTicketStatusForTechnician(
                3L, 77L, TicketStatus.IN_PROGRESS.name(), "Nuova nota", null, "MEDIUM", null);

        assertEquals(77L, response.getTicketID());
        assertEquals("Nuova nota", ticket.getTechnicianNote());
        verify(notificationService).notifyTicketNoteUpdated(5L, 3L, ticket.getTitle(), "T001");
        verify(notificationService, never())
                .notifyTicketNoteUpdated(
                        eq(4L),
                        any(),
                        any(),
                        any());
        verify(ticketTechnicianNoteRepo).save(any(TicketTechnicianNote.class));
    }

    @Test
    void closesTicketWithDistinctTechnicianNoteAndResolution() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setTicketCode("T001");
        ticket.setSpaceID(9L);
        ticket.setDeskID(12L);
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(3L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        ticket.setTechnicianNote("Sostituita lampada.");
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(77L);

        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);
        
        // Simula comportamento state machine per resolve
        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setResolution(invocation.getArgument(1));
            t.setStatus(TicketStatus.RESOLVED.name());
            return null;
        }).when(ticketStateMachine).resolve(any(), anyString());

        ticketService.updateTicketStatusForTechnician(
                3L, 77L, TicketStatus.RESOLVED.name(), "Sostituita lampada.", "Problema risolto.", null, null);

        verify(ticketStateMachine).resolve(ticket, "Problema risolto.");
        verify(ticketStateMachine, never()).verify(any(), anyString());
        assertEquals("Sostituita lampada.", ticket.getTechnicianNote());
        assertEquals("Problema risolto.", ticket.getResolution());
        assertEquals(TicketStatus.RESOLVED.name(), ticket.getStatus());
        verify(notificationService).notifyTicketResolved(5L, "T001");
    }

    @Test
    void notifiesHostWhenTicketGoesToVerifying() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setTicketCode("T001");
        ticket.setSpaceID(9L);
        ticket.setDeskID(12L);
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(3L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        ticket.setTechnicianNote("Nota");
        Space space = new Space();
        space.setSpaceID(9L);
        space.setHostID(4L);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(77L);

        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));
        when(spaceRepo.findById(9L)).thenReturn(Optional.of(space));
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);
        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setResolution(invocation.getArgument(1));
            t.setStatus(TicketStatus.VERIFYING.name());
            return null;
        }).when(ticketStateMachine).verify(any(), anyString());

        ticketService.updateTicketStatusForTechnician(
                3L, 77L, TicketStatus.VERIFYING.name(), "Nota", "Riparato.", null, null);

        verify(ticketStateMachine).verify(ticket, "Riparato.");
        verify(ticketStateMachine, never()).resolve(any(), anyString());
        verify(notificationService).notifyTicketVerifying(5L, "T001");
        verify(notificationService).notifyHostTicketNeedsApproval(4L, "T001");
        verify(notificationService, never())
                .notifyTicketNoteUpdated(
                        eq(4L),
                        any(),
                        any(),
                        any());
    }

    @Test
    void rejectsClosureWithoutResolution() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(77L);
        ticket.setDeskID(12L);
        ticket.setTechnicianID(3L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        when(ticketRepo.findById(77L)).thenReturn(Optional.of(ticket));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ticketService.updateTicketStatusForTechnician(
                        3L, 77L, TicketStatus.RESOLVED.name(), "Nota tecnico", null, null, null));

        assertEquals("La risoluzione è obbligatoria per chiudere la segnalazione.", ex.getMessage());
    }
}
