package it.polimi.smartdesk_backend.service.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.service.ticket.state.TicketStateMachine;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

@ExtendWith(MockitoExtension.class)
class TicketDomainServicesTest {

    private static final Long WORKER_ID = 5L;
    private static final Long HOST_ID = 4L;
    private static final Long SPACE_ID = 9L;
    private static final Long DESK_ID = 12L;
    private static final Long BOOKING_ID = 1L;
    private static final Long TICKET_ID = 77L;

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

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepo,
                deskRepo,
                spaceRepo,
                notificationService,
                ticketResponseService,
                hostOwnershipService,
                hostTechnicianSpaceManagementService,
                ticketStateMachine,
                ticketTechnicianNoteRepo,
                ticketHostNoteRepo,
                ticketCreationService);
    }

  // --- openTicket (delegato a TicketCreationService) ---

    @Test
    void shouldDelegateOpenTicketToCreationService() {
        TicketDTO body = new TicketDTO();
        body.setBookingID(BOOKING_ID);
        body.setTitle("Sedia rotta");
        body.setDescription("La seduta non si blocca");

        Ticket expected = openTicket();
        expected.setTitle("Sedia rotta");
        expected.setTicketCode("TABCD");
        when(ticketCreationService.openTicket(WORKER_ID, body)).thenReturn(expected);

        Ticket created = ticketService.openTicket(WORKER_ID, body);

        assertEquals(expected, created);
        verify(ticketCreationService).openTicket(WORKER_ID, body);
    }

  // --- deleteTicketForRequester ---

    @Test
    void shouldDeleteTicketAsSysAdmin() {
        Ticket ticket = openTicket();
        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicketForRequester(TICKET_ID, 99L, Role.SYS_ADMIN);

        verify(ticketRepo).delete(ticket);
    }

    @Test
    void shouldDeleteOpenTicketAsWorker() {
        Ticket ticket = openTicket();
        ticket.setWorkerID(WORKER_ID);
        ticket.setStatus(TicketStatus.OPEN.name());

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketStateMachine.canAddComment(ticket)).thenReturn(true);

        ticketService.deleteTicketForRequester(TICKET_ID, WORKER_ID, Role.WORKER);

        verify(ticketRepo).delete(ticket);
    }

    @Test
    void shouldRejectWorkerDeleteWhenTicketInProgress() {
        Ticket ticket = openTicket();
        ticket.setWorkerID(WORKER_ID);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        assertThrows(BusinessRuleException.class,
                () -> ticketService.deleteTicketForRequester(TICKET_ID, WORKER_ID, Role.WORKER));
        verify(ticketRepo, never()).delete(any(Ticket.class));
    }

  // --- host lifecycle ---

    @Test
    void shouldApproveTicketAsHost() {
        Ticket ticket = openTicket();
        ticket.setDeskID(DESK_ID);
        ticket.setWorkerID(WORKER_ID);
        ticket.setTicketCode("T1234");
        TicketResponseDTO dto = new TicketResponseDTO();

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        doNothing().when(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, DESK_ID);
        doNothing().when(ticketStateMachine).approve(ticket);
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO result = ticketService.hostApproveTicket(HOST_ID, TICKET_ID);

        assertEquals(dto, result);
        verify(notificationService).notifyTicketResolved(WORKER_ID, "T1234");
    }

    @Test
    void shouldRejectTicketAsHostWithoutReassigningTechnician() {
        Ticket ticket = openTicket();
        ticket.setDeskID(DESK_ID);
        ticket.setSpaceID(SPACE_ID);
        ticket.setWorkerID(WORKER_ID);
        ticket.setTechnicianID(8L);
        ticket.setTicketCode("T5678");
        TicketResponseDTO dto = new TicketResponseDTO();

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        doNothing().when(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, DESK_ID);
        doNothing().when(ticketStateMachine).reject(ticket);
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO result = ticketService.hostRejectTicket(HOST_ID, TICKET_ID, null, "Needs more work");

        assertEquals(dto, result);
        verify(ticketHostNoteRepo).save(any());
        verify(notificationService).notifyTicketInProgress(WORKER_ID, "T5678");
        verify(hostTechnicianSpaceManagementService, never())
                .ensureTechnicianLinkedToSpace(anyLong(), anyLong(), anyLong());
    }

    @Test
    void shouldDismissDeskAndCloseTicketAsHost() {
        Ticket ticket = openTicket();
        ticket.setDeskID(DESK_ID);
        TicketResponseDTO dto = new TicketResponseDTO();

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        doNothing().when(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, DESK_ID);
        doNothing().when(ticketStateMachine).close(ticket);
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO result = ticketService.hostDismissDesk(HOST_ID, TICKET_ID);

        assertEquals(dto, result);
        verify(ticketStateMachine).close(ticket);
        verify(ticketHostNoteRepo).save(any());
    }

  // --- purge ---

    @Test
    void shouldPurgeOldResolvedTickets() {
        when(ticketRepo.deleteByStatusAndResolvedAtBefore(eq(TicketStatus.RESOLVED.name()), any(LocalDateTime.class)))
                .thenReturn(3);

        int removed = ticketService.purgeResolvedTicketsOlderThan(Duration.ofDays(30));

        assertEquals(3, removed);
    }

    @Test
    void shouldClearResolvedHistoryForHost() {
        when(ticketRepo.deleteByStatusAndDeskHost(TicketStatus.RESOLVED.name(), HOST_ID)).thenReturn(2);

        assertEquals(2, ticketService.clearResolvedTicketHistoryForHost(HOST_ID));
    }

  // --- queries ---

    @Test
    void shouldReturnTicketForWorkerRequester() {
        Ticket ticket = openTicket();
        ticket.setWorkerID(WORKER_ID);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        Ticket loaded = ticketService.getTicketByIdForRequester(TICKET_ID, WORKER_ID, Role.WORKER);

        assertEquals(TICKET_ID, loaded.getTicketID());
    }

    @Test
    void shouldDenyTicketAccessToOtherWorker() {
        Ticket ticket = openTicket();
        ticket.setWorkerID(99L);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        assertThrows(NotFoundException.class,
                () -> ticketService.getTicketByIdForRequester(TICKET_ID, WORKER_ID, Role.WORKER));
    }

    @Test
    void shouldDelegateOpenTicketByDeskCodeToCreationService() {
        TicketDTO body = new TicketDTO();
        body.setDeskCode("desk A1");
        body.setTitle("Problema elettrico");
        body.setDescription("Presa non funzionante");

        Ticket expected = openTicket();
        expected.setTitle("Problema elettrico");
        expected.setTicketCode("TXYZ1");
        when(ticketCreationService.openTicket(WORKER_ID, body)).thenReturn(expected);

        Ticket created = ticketService.openTicket(WORKER_ID, body);

        assertEquals(expected, created);
        verify(ticketCreationService).openTicket(WORKER_ID, body);
    }

    @Test
    void shouldListResolvedTicketsForHostWithClampedLimit() {
        Ticket old = openTicket();
        old.setResolvedAt(LocalDateTime.now().minusDays(1));
        Ticket recent = openTicket();
        recent.setTicketID(88L);
        recent.setResolvedAt(LocalDateTime.now());

        when(ticketRepo.findResolvedHistoryForHost(HOST_ID)).thenReturn(List.of(old, recent));
        when(ticketResponseService.toResponseDTOList(any())).thenReturn(List.of(new TicketResponseDTO(), new TicketResponseDTO()));

        List<TicketResponseDTO> result = ticketService.getResolvedTicketsForHost(HOST_ID, 500);

        assertEquals(2, result.size());
    }

    @Test
    void shouldHostRejectAndReassignTechnician() {
        Ticket ticket = openTicket();
        ticket.setDeskID(DESK_ID);
        ticket.setSpaceID(SPACE_ID);
        ticket.setWorkerID(WORKER_ID);
        ticket.setTechnicianID(8L);
        ticket.setTicketCode("T9999");
        TicketResponseDTO dto = new TicketResponseDTO();

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        doNothing().when(hostOwnershipService).assertDeskOwnedByHostOrNotFound(HOST_ID, DESK_ID);
        when(ticketRepo.save(ticket)).thenReturn(ticket);
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        TicketResponseDTO result = ticketService.hostRejectTicket(HOST_ID, TICKET_ID, 11L, null);

        assertEquals(dto, result);
        verify(hostTechnicianSpaceManagementService).ensureTechnicianLinkedToSpace(HOST_ID, SPACE_ID, 11L);
        verify(ticketStateMachine).assignTechnician(ticket, 11L);
    }

    @Test
    void shouldDelegateWorkerTicketListingToRepository() {
        when(ticketRepo.findVisibleToWorker(eq(WORKER_ID), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of());
        when(ticketResponseService.toResponseDTOList(List.of())).thenReturn(List.of());

        assertTrue(ticketService.getTicketResponsesByWorker(WORKER_ID).isEmpty());
    }

    @Test
    void shouldListTicketsByDesk() {
        Ticket ticket = openTicket();
        when(ticketRepo.findByDeskID(DESK_ID)).thenReturn(List.of(ticket));
        when(ticketResponseService.toResponseDTOList(List.of(ticket))).thenReturn(List.of(new TicketResponseDTO()));

        assertEquals(1, ticketService.getTicketsByDesk(DESK_ID).size());
    }

    @Test
    void shouldReturnTicketResponseForWorkerRequester() {
        Ticket ticket = openTicket();
        ticket.setWorkerID(WORKER_ID);
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(TICKET_ID);

        when(ticketRepo.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketResponseService.toResponseDTO(ticket)).thenReturn(dto);

        assertEquals(TICKET_ID, ticketService.getTicketResponseByIdForRequester(TICKET_ID, WORKER_ID, Role.WORKER).getTicketID());
    }

    @Test
    void shouldClearResolvedHistoryForTechnician() {
        when(ticketRepo.deleteResolvedHistoryForTechnician(8L)).thenReturn(4);

        assertEquals(4, ticketService.clearResolvedTicketHistoryForTechnician(8L));
    }

    private Desk deskInSpace() {
        Space space = EntityTestFixtures.spaceMilano(SPACE_ID, HOST_ID);
        Desk desk = new Desk();
        desk.setDeskID(DESK_ID);
        desk.setCode("A1");
        desk.setSpace(space);
        return desk;
    }

    private Ticket openTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(TICKET_ID);
        return ticket;
    }
}
