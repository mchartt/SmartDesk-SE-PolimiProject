package it.polimi.smartdesk_backend.controller.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.booking.SlotStatusDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceClosureDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.DeskAvailabilityService;
import it.polimi.smartdesk_backend.service.space.SpaceClosureService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(WorkerSpaceController.class)
@Import(RestExceptionHandler.class)
class WorkerSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceManagementService spaceManagementService;
    @MockitoBean
    private SpaceClosureService spaceClosureService;
    @MockitoBean
    private DeskAvailabilityService deskAvailabilityService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(4L, Role.WORKER));
    }

    @Test
    void workerApprovedSpacesList() throws Exception {
        var space = new SpaceDTO();
        space.setSpaceID(22L);
        space.setName("Hub Milano");
        when(spaceManagementService.findAll()).thenReturn(List.of(space));

        mockMvc.perform(get("/api/workers/spaces")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(22));
    }

    @Test
    void workerReadsSpaceClosureByDate() throws Exception {
        var closure = new SpaceClosureDTO(3L, 22L, LocalDate.parse("2026-04-28"), "Manutenzione");
        when(spaceClosureService.findForWorker(22L, LocalDate.parse("2026-04-28"))).thenReturn(Optional.of(closure));

        mockMvc.perform(get("/api/workers/spaces/{spaceId}/closures", 22L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .param("date", "2026-04-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void workerSpacesAndSlots() throws Exception {
        SlotStatusDTO slot = new SlotStatusDTO("09:00-10:00", "AVAILABLE");
        when(deskAvailabilityService.getSlotAvailability(10L, LocalDate.parse("2026-04-28"))).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/workers/desks/{deskID}/slots", 10L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .param("date", "2026-04-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }
}
