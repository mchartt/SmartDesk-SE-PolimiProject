package it.polimi.smartdesk_backend.controller.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.ticket.TicketCommentService;
import it.polimi.smartdesk_backend.service.ticket.TicketService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(WorkerTicketController.class)
@Import(RestExceptionHandler.class)
class WorkerTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private TicketCommentService ticketCommentService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(4L, Role.WORKER));
    }

    @Test
    void workerOpensTicket() throws Exception {
        mockMvc.perform(post("/api/workers/tickets")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "deskCode": "10",
                          "title": "Monitor rotto",
                          "description": "Broken"
                        }
                        """))
                .andExpect(status().isCreated());

        verify(ticketService).openTicket(eq(4L), any());
    }

    @Test
    void workerMyTickets() throws Exception {
        TicketResponseDTO response = new TicketResponseDTO();
        response.setTicketID(77L);
        when(ticketService.getTicketResponsesByWorker(4L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/workers/tickets")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketID").value(77));
    }

    @Test
    void workerOwnTicketDetail() throws Exception {
        TicketResponseDTO response = new TicketResponseDTO();
        response.setTicketID(33L);
        when(ticketService.getTicketResponseByIdForRequester(33L, 4L, Role.WORKER)).thenReturn(response);

        mockMvc.perform(get("/api/workers/tickets/{ticketID}", 33L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketID").value(33));
    }

    @Test
    void workerDeletesOwnTicket() throws Exception {
        mockMvc.perform(delete("/api/workers/tickets/{ticketID}", 33L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(ticketService).deleteTicketForRequester(33L, 4L, Role.WORKER);
    }
}
