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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.host.HostTechnicianAccountService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianDashboardService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostTechnicianController.class)
@Import(RestExceptionHandler.class)
class HostTechnicianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostTechnicianAccountService hostTechnicianAccountService;
    @MockitoBean
    private HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;
    @MockitoBean
    private HostTechnicianDashboardService hostTechnicianDashboardService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }

    @Test
    void technicianCycleOnSpace() throws Exception {
        TechnicianDTO technician = new TechnicianDTO();
        technician.setTechnicianID(22L);
        technician.setEmail("tech@sd.it");

        when(hostTechnicianAccountService.createTechnician(eq(4L), any())).thenReturn(technician);
        when(hostTechnicianSpaceManagementService.assignTechnicianToSpace(4L, 10L, 22L)).thenReturn(technician);

        mockMvc.perform(post("/api/hosts/technicians")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Mario","email":"tech@sd.it","password":"Secret123!","specialization":"General"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technicianID").value(22));

        mockMvc.perform(post("/api/hosts/spaces/10/technicians/22")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/hosts/spaces/10/technicians/22")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isNoContent());

        verify(hostTechnicianSpaceManagementService).unassignTechnicianFromSpace(4L, 10L, 22L);
    }
}
