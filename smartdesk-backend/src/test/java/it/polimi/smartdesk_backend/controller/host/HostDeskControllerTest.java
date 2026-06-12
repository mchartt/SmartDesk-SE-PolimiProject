package it.polimi.smartdesk_backend.controller.host;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.host.HostDeskService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostDeskController.class)
@Import(RestExceptionHandler.class)
class HostDeskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostDeskService hostDeskService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }

    @Test
    void crudDeskHost() throws Exception {
        DeskDTO desk = new DeskDTO(55L, "B1", "B1", List.of("wifi"));
        when(hostDeskService.createDesk(eq(4L), any())).thenReturn(desk);
        when(hostDeskService.updateDeskForHost(eq(4L), eq(55L), any())).thenReturn(desk);

        mockMvc.perform(post("/api/hosts/desks")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"B1","roomID":3,"amenities":["wifi"],"spaceID":10}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55));

        mockMvc.perform(put("/api/hosts/desks/{deskID}", 55L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"B1","roomID":3,"amenities":["wifi"],"spaceID":10}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/hosts/desks/{deskID}", 55L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isNoContent());

        verify(hostDeskService).removeDeskForHost(4L, 55L);
    }
}
