package it.polimi.smartdesk_backend.controller.technician;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianAssignedSpaceDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TechnicianAssignmentService;
import it.polimi.smartdesk_backend.service.desk.TechnicianDeskMaintenanceService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Area tecnico: ticket assegnati, aggiornamento stato e spazi collegati. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(TechnicianController.class)
@Import({RestExceptionHandler.class})
class TechnicianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketCommentService ticketCommentService;

    @MockitoBean
    private TechnicianAssignmentService technicianAssignmentService;

    @MockitoBean
    private TechnicianDeskMaintenanceService technicianDeskMaintenanceService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(7L, Role.TECHNICIAN));
    }

    @Test
    void technicianMyAssignedSpaces() throws Exception {
        TechnicianAssignedSpaceDTO row = new TechnicianAssignedSpaceDTO();
        row.setSpaceID(3L);
        row.setName("Milano Central");
        row.setOfficeCode("ABC123");

        when(technicianAssignmentService.listAssignedSpaces(7L)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/technicians/spaces")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(3))
                .andExpect(jsonPath("$[0].name").value("Milano Central"))
                .andExpect(jsonPath("$[0].officeCode").value("ABC123"));
        verify(technicianAssignmentService).listAssignedSpaces(7L);
    }

    @Test
    void technicianListAssignedSpaceDesks() throws Exception {
        DeskDTO desk = new DeskDTO();
        desk.setId(15L);
        desk.setCode("D15");
        desk.setCurrentState("AVAILABLE");

        when(technicianAssignmentService.listAssignedDesks(7L, 3L)).thenReturn(List.of(desk));

        mockMvc.perform(get("/api/technicians/spaces/{spaceID}/desks", 3L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].code").value("D15"));

        verify(technicianAssignmentService).listAssignedDesks(7L, 3L);
    }

    @Test
    void getPendingAndAssignedTickets() throws Exception {
        TicketResponseDTO pending = new TicketResponseDTO();
        pending.setTicketID(1L);
        pending.setStatus("OPEN");

        TicketResponseDTO assigned = new TicketResponseDTO();
        assigned.setTicketID(2L);
        assigned.setStatus("IN_PROGRESS");

        when(ticketService.getPendingTicketResponses(7L)).thenReturn(List.of(pending));
        when(ticketService.getTicketResponsesByTechnician(7L)).thenReturn(List.of(assigned));

        mockMvc.perform(get("/api/technicians/tickets/pending")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketID").value(1));
        verify(ticketService).getPendingTicketResponses(7L);

        mockMvc.perform(get("/api/technicians/tickets/assigned")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketID").value(2));
    }

    @Test
    void technicianAddsTicketComment() throws Exception {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(44L);
        dto.setStatus("IN_PROGRESS");
        when(ticketCommentService.addComment(7L, 44L, Role.TECHNICIAN, "Intervento in corso"))
                .thenReturn(dto);

        mockMvc.perform(post("/api/technicians/tickets/{ticketID}/comments", 44L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"body":"Intervento in corso"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketID").value(44));

        verify(ticketCommentService).addComment(7L, 44L, Role.TECHNICIAN, "Intervento in corso");
    }

    @Test
    void technicianUpdatesTicketAndMaintenance() throws Exception {
        TicketResponseDTO dtoView = new TicketResponseDTO();
        dtoView.setTicketID(44L);
        dtoView.setStatus("IN_PROGRESS");

        TicketResponseDTO dtoResolved = new TicketResponseDTO();
        dtoResolved.setTicketID(44L);
        dtoResolved.setStatus("RESOLVED");

        when(ticketService.getTicketResponseByIdForRequester(44L, 7L, Role.TECHNICIAN)).thenReturn(dtoView);
        when(ticketService.updateTicketStatusForTechnician(7L, 44L, "RESOLVED", "Nota tecnico", "Intervento completato.", null, null))
                .thenReturn(dtoResolved);

        mockMvc.perform(get("/api/technicians/tickets/{ticketID}", 44L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketID").value(44));

        mockMvc.perform(patch("/api/technicians/tickets/{ticketID}", 44L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"RESOLVED","note":"Nota tecnico","resolution":"Intervento completato."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(patch("/api/technicians/desks/{deskID}/maintenance", 15L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/technicians/desks/{deskID}/maintenance", 15L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isNoContent());

        verify(technicianDeskMaintenanceService).setDeskMaintenanceForTechnician(7L, 15L);
        verify(technicianDeskMaintenanceService).revertDeskMaintenanceForTechnician(7L, 15L);
    }

    @Test
    void technicianClearsResolvedHistory() throws Exception {
        when(ticketService.clearResolvedTicketHistoryForTechnician(7L)).thenReturn(3);

        mockMvc.perform(delete("/api/technicians/tickets/resolved-history")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(3));

        verify(ticketService).clearResolvedTicketHistoryForTechnician(7L);
    }
}

