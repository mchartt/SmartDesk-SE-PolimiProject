package it.polimi.smartdesk_backend.controller.space;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import it.polimi.smartdesk_backend.util.message.AuthMessage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Catalogo spazi pubblico/semipubblico: risposte JSON e anonimo non autorizzato induve serve. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(SpaceController.class)
@Import({RestExceptionHandler.class})
class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceManagementService SpaceManagementService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(5L, Role.WORKER));
    }

    @Test
    void publicSpacesList() throws Exception {
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(21L);
        dto.setName("Main Space");
        when(SpaceManagementService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/spaces")
                .principal(SecurityTestUtils.authenticatedUser(5L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(21));

        verify(accessControlService).assertAuthenticated(any());
    }

    @Test
    void spaceDetailById() throws Exception {
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(33L);
        dto.setName("Quiet room");
        when(SpaceManagementService.findById(33L)).thenReturn(dto);

        mockMvc.perform(get("/api/spaces/{spaceId}", 33L)
                .principal(SecurityTestUtils.authenticatedUser(5L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Quiet room"));

        verify(SpaceManagementService).findById(33L);
    }

    @Test
    void spacesListWithoutToken() throws Exception {
        when(accessControlService.assertAuthenticated(any()))
                .thenThrow(new UnauthorizedException(AuthMessage.AUTHENTICATION_REQUIRED.text()));

        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isUnauthorized());
    }
}

