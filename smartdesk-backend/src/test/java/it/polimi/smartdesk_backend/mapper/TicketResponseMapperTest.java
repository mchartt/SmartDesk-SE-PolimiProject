package it.polimi.smartdesk_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.polimi.smartdesk_backend.dto.common.SpaceSummary;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.model.user.Worker;

class TicketResponseMapperTest {

    private final TicketResponseMapper mapper = new TicketResponseMapper();

    @Test
    void shouldMapTicketWithDeskSpaceUsersAndStoredNotes() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 28, 10, 0);
        Ticket ticket = ticket(now);

        Worker worker = new Worker();
        worker.setId(5L);
        worker.setName("Anna");
        worker.setSurname("Bianchi");
        worker.setEmail("anna@test.it");

        Technician tech = new Technician();
        tech.setId(8L);
        tech.setName("Marco");
        tech.setSurname("Verdi");

        TicketTechnicianNote techNote = TicketTechnicianNote.of(1L, 8L, "Fixed outlet", now);
        TicketWorkerNote workerNote = TicketWorkerNote.of(1L, 5L, "Problema elettrico", now.minusHours(1));
        TicketHostNote hostNote = TicketHostNote.of(1L, 4L, "Checking with vendor", now.minusMinutes(30));

        TicketResponseDTO dto = mapper.buildResponseDTO(
                ticket,
                Map.of(12L, "A1"),
                Map.of(9L, new SpaceSummary("Hub Milano", "OFF-9")),
                Map.of(5L, worker, 8L, tech),
                Map.of(1L, List.of(techNote)),
                Map.of(1L, List.of(workerNote)),
                Map.of(1L, List.of(hostNote)));

        assertEquals(1L, dto.getTicketID());
        assertEquals("T1234", dto.getTicketCode());
        assertEquals("A1", dto.getDeskCode());
        assertEquals("Hub Milano", dto.getSpaceName());
        assertEquals("OFF-9", dto.getOfficeCode());
        assertEquals("Anna", dto.getWorkerName());
        assertEquals("Bianchi", dto.getWorkerSurname());
        assertEquals("Marco", dto.getAssignedTechName());
        assertEquals(1, dto.getTechnicianNoteHistory().size());
        assertEquals(1, dto.getWorkerNoteHistory().size());
        assertEquals(1, dto.getHostNoteHistory().size());
        assertTrue(dto.getTechnicianNoteHistory().get(0).getAuthorLabel().contains("Marco"));
    }

    @Test
    void shouldFallBackToLegacyTechnicianNoteWhenNoStoredNotes() {
        LocalDateTime created = LocalDateTime.of(2026, 5, 28, 9, 0);
        Ticket ticket = ticket(created);
        ticket.setTechnicianNote("Testo di risoluzione precedente");
        ticket.setResolvedAt(created.plusHours(2));

        Technician tech = new Technician();
        tech.setId(8L);
        tech.setName("Tech");

        TicketResponseDTO dto = mapper.buildResponseDTO(
                ticket,
                Map.of(),
                Map.of(),
                Map.of(8L, tech),
                Map.of(),
                Map.of(),
                Map.of());

        assertEquals(1, dto.getTechnicianNoteHistory().size());
        assertEquals("Testo di risoluzione precedente", dto.getTechnicianNoteHistory().get(0).getBody());
    }

    @Test
    void shouldUseRoleLabelWhenAuthorUserMissing() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = ticket(now);
        TicketWorkerNote workerNote = TicketWorkerNote.of(1L, 99L, "Anonymous report", now);

        TicketResponseDTO dto = mapper.buildResponseDTO(
                ticket,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(1L, List.of(workerNote)),
                Map.of());

        assertEquals("Utente", dto.getWorkerNoteHistory().get(0).getAuthorLabel());
    }

    @Test
    void shouldReturnEmptyHistoriesWhenTicketIdIsNull() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Bozza");

        TicketResponseDTO dto = mapper.buildResponseDTO(
                ticket,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(1L, List.of(TicketTechnicianNote.of(1L, 8L, "x", LocalDateTime.now()))),
                Map.of(1L, List.of(TicketWorkerNote.of(1L, 5L, "y", LocalDateTime.now()))),
                Map.of(1L, List.of(TicketHostNote.of(1L, 4L, "z", LocalDateTime.now()))));

        assertTrue(dto.getTechnicianNoteHistory().isEmpty());
        assertTrue(dto.getWorkerNoteHistory().isEmpty());
        assertTrue(dto.getHostNoteHistory().isEmpty());
    }

    private static Ticket ticket(LocalDateTime createdAt) {
        Ticket ticket = new Ticket();
        ticket.setTicketID(1L);
        ticket.setTicketCode("T1234");
        ticket.setTitle("Problema elettrico");
        ticket.setDescription("Presa guasta");
        ticket.setStatus("IN_PROGRESS");
        ticket.setDeskID(12L);
        ticket.setSpaceID(9L);
        ticket.setWorkerID(5L);
        ticket.setTechnicianID(8L);
        ticket.setSeverity("HIGH");
        ticket.setCreatedAt(createdAt);
        ticket.setEstimatedResolutionAt(createdAt.plusDays(1));
        return ticket;
    }
}
