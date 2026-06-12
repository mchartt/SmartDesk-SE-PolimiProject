package it.polimi.smartdesk_backend.service.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.mapper.TicketResponseMapper;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TicketResponseServiceTest {

    @Mock
    private TicketResponseMapper ticketResponseMapper;
    @Mock
    private DeskRepository deskRepo;
    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketTechnicianNoteRepository ticketTechnicianNoteRepo;
    @Mock
    private TicketWorkerNoteRepository ticketWorkerNoteRepo;
    @Mock
    private TicketHostNoteRepository ticketHostNoteRepo;

    @InjectMocks
    private TicketResponseService ticketResponseService;

    @Test
    void shouldReturnEmptyListForNoTickets() {
        assertTrue(ticketResponseService.toResponseDTOList(List.of()).isEmpty());
    }

    @Test
    void shouldBuildResponseDtoWithNotesAndRelatedEntities() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(1L);
        ticket.setDeskID(12L);
        ticket.setSpaceID(9L);
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(8L);

        Desk desk = new Desk();
        desk.setDeskID(12L);
        desk.setCode("A1");
        Space space = new Space();
        space.setSpaceID(9L);
        space.setName("Hub");
        Worker worker = new Worker();
        worker.setId(5L);
        worker.setName("Anna");
        Technician tech = new Technician();
        tech.setId(8L);
        tech.setName("Tech");

        TicketTechnicianNote techNote = TicketTechnicianNote.of(1L, 8L, "Fixed", LocalDateTime.now());
        TicketWorkerNote workerNote = TicketWorkerNote.of(1L, 5L, "Issue detail", LocalDateTime.now());
        TicketHostNote hostNote = TicketHostNote.of(1L, 4L, "Checking", LocalDateTime.now());

        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(1L);

        when(ticketTechnicianNoteRepo.findByTicketIDInOrderByCreatedAtAsc(Set.of(1L))).thenReturn(List.of(techNote));
        when(ticketWorkerNoteRepo.findByTicketIDInOrderByCreatedAtAsc(Set.of(1L))).thenReturn(List.of(workerNote));
        when(ticketHostNoteRepo.findByTicketIDInOrderByCreatedAtAsc(Set.of(1L))).thenReturn(List.of(hostNote));
        when(deskRepo.findAllById(Set.of(12L))).thenReturn(List.of(desk));
        when(spaceRepo.findAllById(Set.of(9L))).thenReturn(List.of(space));
        when(userRepository.findAllById(any())).thenReturn(List.of(worker, tech));
        when(ticketResponseMapper.buildResponseDTO(
                eq(ticket), any(), any(), any(), any(), any(), any())).thenReturn(dto);

        List<TicketResponseDTO> result = ticketResponseService.toResponseDTOList(List.of(ticket));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTicketID());
    }

    @Test
    void shouldBuildSingleResponseDto() {
        Ticket ticket = new Ticket();
        ticket.setTicketID(2L);

        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(2L);

        when(ticketTechnicianNoteRepo.findByTicketIDInOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketWorkerNoteRepo.findByTicketIDInOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketHostNoteRepo.findByTicketIDInOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketResponseMapper.buildResponseDTO(
                eq(ticket), any(), any(), any(), any(), any(), any())).thenReturn(dto);

        assertEquals(2L, ticketResponseService.toResponseDTO(ticket).getTicketID());
    }
}
