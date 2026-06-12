package it.polimi.smartdesk_backend.controller.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.booking.WaitlistStatusDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.BookingWaitlistService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(WorkerWaitlistController.class)
@Import(RestExceptionHandler.class)
class WorkerWaitlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingWaitlistService bookingWaitlistService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(4L, Role.WORKER));
    }

    @Test
    void workerSubscribesWaitlist() throws Exception {
        mockMvc.perform(post("/api/workers/desks/{deskID}/waitlist", 10L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .param("date", "2026-04-28"))
                .andExpect(status().isNoContent());

        verify(bookingWaitlistService).notifyMeWhenAvailable(10L, LocalDate.parse("2026-04-28"), 4L, null, null);
    }

    @Test
    void workerWaitlistStatus() throws Exception {
        WaitlistStatusDTO statusDTO = new WaitlistStatusDTO();
        statusDTO.setDeskID(10L);
        statusDTO.setDate(LocalDate.parse("2026-04-28"));
        statusDTO.setSubscribed(true);
        statusDTO.setNotified(false);
        when(bookingWaitlistService.getWaitlistStatus(10L, LocalDate.parse("2026-04-28"), 4L)).thenReturn(statusDTO);

        mockMvc.perform(get("/api/workers/desks/{deskID}/waitlist", 10L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .param("date", "2026-04-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true));
    }
}
