package it.polimi.smartdesk_backend.service.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
class TicketCommentServiceTest {

    @Mock
    private TicketRepository ticketRepo;
    @Mock
    private TicketWorkerNoteRepository ticketWorkerNoteRepo;
    @Mock
    private TicketTechnicianNoteRepository ticketTechnicianNoteRepo;
    @Mock
    private TicketHostNoteRepository ticketHostNoteRepo;
    @Mock
    private TicketResponseService ticketResponseService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private HostOwnershipService hostOwnershipService;

    @InjectMocks
    private TicketCommentService ticketCommentService;

    @Test
    void shouldAddWorkerCommentAndNotifyTechnician() {
        Ticket ticket = ticket();
        ticket.setTechnicianID(8L);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(1L);

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO result = ticketCommentService.addComment(5L, 1L, Role.WORKER, "Dettagli aggiornati dal worker");

        assertEquals(1L, result.getTicketID());
        verify(ticketWorkerNoteRepo).save(any());
        verify(notificationService, never()).notifyTicketNoteUpdated(any(), any(), any(), any());
    }

    @Test
    void shouldNotifyWorkerWhenTechnicianComments() {
        Ticket ticket = ticket();
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(8L);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(1L);

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        ticketCommentService.addComment(8L, 1L, Role.TECHNICIAN, "Aggiornamento del tecnico sul ticket");

        verify(ticketTechnicianNoteRepo).save(any());
        verify(notificationService).notifyTicketNoteUpdated(eq(5L), eq(8L), eq("Problema"), eq("T1000"));
    }

    @Test
    void shouldAddHostComment() {
        Ticket ticket = ticket();
        ticket.setDeskID(12L);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(1L);

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        ticketCommentService.addComment(4L, 1L, Role.HOST, "Nota di follow-up dell'host");

        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(4L, 12L);
        verify(ticketHostNoteRepo).save(any());
    }

    @Test
    void shouldRejectCommentOnClosedTicket() {
        Ticket ticket = ticket();
        ticket.setStatus(TicketStatus.CLOSED.name());

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(BusinessRuleException.class,
                () -> ticketCommentService.addComment(5L, 1L, Role.WORKER, "Troppo tardi"));
    }

    @Test
    void shouldRejectCommentFromOtherWorker() {
        Ticket ticket = ticket();
        ticket.setWorkerID(99L);

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(NotFoundException.class,
                () -> ticketCommentService.addComment(5L, 1L, Role.WORKER, "Non è mio"));
    }

    private static Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(1L);
        ticket.setWorkerID(5L);
        ticket.setDeskID(12L);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        ticket.setTicketCode("T1000");
        ticket.setTitle("Problema");
        return ticket;
    }
}
