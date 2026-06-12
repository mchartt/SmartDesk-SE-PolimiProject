package it.polimi.smartdesk_backend.controller.desk;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.desk.DeskService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Consultazione desk via API host con MockMvc (slice controller). */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(DeskController.class)
@Import(RestExceptionHandler.class)
class DeskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeskService deskService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubAuthenticated(accessControlService, 4L, Role.WORKER);
    }

    @Test
    void allDesksWithoutFilter() throws Exception {
        when(deskService.findAllApproved()).thenReturn(List.of(new DeskDTO(10L, "A1", "Building A", List.of("wifi"))));

        mockMvc.perform(get("/api/desks")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));

        verify(accessControlService).assertAuthenticated(any());
        verify(deskService).findAllApproved();
    }

    @Test
    void desksFilteredBySpace() throws Exception {
        when(deskService.findBySpace(7L)).thenReturn(List.of(new DeskDTO(11L, "B1", "Building B", List.of("monitor"))));

        mockMvc.perform(get("/api/desks")
                .param("spaceId", "7")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));

        verify(deskService).findBySpace(7L);
    }

    @Test
    void hostSeesDeskOfOwnSpaceEvenIfPending() throws Exception {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(9L, Role.HOST));
        when(deskService.findBySpaceForHost(9L, 7L))
                .thenReturn(List.of(new DeskDTO(15L, "H1", "Host Building", List.of("wifi"))));

        mockMvc.perform(get("/api/desks")
                .param("spaceId", "7")
                .principal(SecurityTestUtils.authenticatedUser(9L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15));

        verify(deskService).findBySpaceForHost(9L, 7L);
    }

    @Test
    void deskById() throws Exception {
        when(deskService.findApprovedById(12L)).thenReturn(new DeskDTO(12L, "C1", "Building C", List.of("dual-screen")));

        mockMvc.perform(get("/api/desks/{deskId}", 12L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.building").value("Building C"));
    }

    @Test
    void availableDesksByDay() throws Exception {
        when(deskService.findAvailable(LocalDate.of(2026, 4, 25)))
                .thenReturn(List.of(new DeskDTO(14L, "D1", "Building D", List.of("wifi", "quiet"))));

        mockMvc.perform(get("/api/desks/available")
                .param("date", "2026-04-25")
                .param("start", "2026-04-25T09:00:00")
                .param("end", "2026-04-25T18:00:00")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(14));

        verify(deskService).findAvailable(LocalDate.of(2026, 4, 25));
    }
}
