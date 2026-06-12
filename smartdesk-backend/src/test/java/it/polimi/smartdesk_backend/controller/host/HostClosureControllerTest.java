package it.polimi.smartdesk_backend.controller.host;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.space.SpaceClosureService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostClosureController.class)
@Import(RestExceptionHandler.class)
class HostClosureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceClosureService spaceClosureService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }

    @Test
    void listClosures() throws Exception {
        when(spaceClosureService.listForHost(4L, 7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/hosts/spaces/7/closures")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());
    }
}
