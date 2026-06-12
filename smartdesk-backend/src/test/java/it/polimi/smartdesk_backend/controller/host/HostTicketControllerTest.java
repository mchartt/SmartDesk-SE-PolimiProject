package it.polimi.smartdesk_backend.controller.host;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostTicketController.class)
@Import(RestExceptionHandler.class)
class HostTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private TicketCommentService ticketCommentService;
    @MockitoBean
    private HostOwnershipService hostOwnershipService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }

    @Test
    void ticketByDesk() throws Exception {
        TicketResponseDTO response = new TicketResponseDTO();
        response.setTicketID(31L);
        when(ticketService.getTicketsByDesk(55L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/hosts/desks/{deskId}/tickets", 55L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketID").value(31));

        verify(hostOwnershipService).assertDeskOwnedByHostOrNotFound(4L, 55L);
    }

    @Test
    void assignApproveRejectAndClearResolvedTickets() throws Exception {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(31L);

        when(ticketService.assignTechnicianToTicket(eq(4L), eq(31L), eq(8L), any())).thenReturn(dto);
        when(ticketService.hostApproveTicket(4L, 31L)).thenReturn(dto);
        when(ticketService.hostRejectTicket(eq(4L), eq(31L), eq(null), any())).thenReturn(dto);
        when(ticketService.hostDismissDesk(4L, 31L)).thenReturn(dto);
        when(ticketService.getResolvedTicketsForHost(4L, 80)).thenReturn(List.of(dto));
        when(ticketService.clearResolvedTicketHistoryForHost(4L)).thenReturn(2);
        when(ticketCommentService.addComment(eq(4L), eq(31L), eq(Role.HOST), any())).thenReturn(dto);

        mockMvc.perform(post("/api/hosts/tickets/31/technicians/8")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"HIGH\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hosts/tickets/31/approve")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hosts/tickets/31/reject")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not fixed\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hosts/tickets/31/comments")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Checking again\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hosts/tickets/31/dismiss-desk")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hosts/resolved-tickets")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketID").value(31));

        mockMvc.perform(delete("/api/hosts/resolved-tickets")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));
    }
}
