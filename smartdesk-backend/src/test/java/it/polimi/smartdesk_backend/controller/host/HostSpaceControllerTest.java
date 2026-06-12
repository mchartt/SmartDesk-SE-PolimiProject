package it.polimi.smartdesk_backend.controller.host;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import it.polimi.smartdesk_backend.dto.space.RoomDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.host.HostAmenityPresetService;
import it.polimi.smartdesk_backend.service.host.HostRoomService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostSpaceController.class)
@Import(RestExceptionHandler.class)
class HostSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceManagementService spaceManagementService;
    @MockitoBean
    private HostRoomService hostRoomService;
    @MockitoBean
    private HostAmenityPresetService hostAmenityPresetService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }

    @Test
    void shouldRunHostSpaceCrudCycle() throws Exception {
        SpaceDTO dto = new SpaceDTO();
        dto.setSpaceID(10L);
        dto.setName("Space A");

        when(spaceManagementService.createSpace(eq(4L), any())).thenReturn(dto);
        when(spaceManagementService.updateSpaceForHost(eq(4L), eq(10L), any())).thenReturn(dto);

        mockMvc.perform(post("/api/hosts")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Space A","address":"Addr","city":"Milano","description":"Desc"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceID").value(10));

        mockMvc.perform(put("/api/hosts/spaces/{spaceId}", 10L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Space A","address":"Addr","city":"Milano","description":"Desc"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/hosts/spaces/{spaceId}", 10L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isNoContent());

        verify(spaceManagementService).deleteSpaceForHost(4L, 10L);
    }

    @Test
    void shouldListRoomsAndAmenityPresets() throws Exception {
        RoomDTO room = new RoomDTO();
        room.setRoomID(11L);
        room.setName("Open Space");

        when(hostRoomService.listRoomsForHost(4L, 7L)).thenReturn(List.of(room));
        when(hostAmenityPresetService.listAmenityPresetsForHost(4L, 7L)).thenReturn(List.of());
        when(spaceManagementService.findByHost(4L)).thenReturn(List.of(new SpaceDTO()));

        mockMvc.perform(get("/api/hosts/spaces/7/rooms")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Open Space"));

        mockMvc.perform(get("/api/hosts/spaces/7/amenity-presets")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hosts/4/spaces")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk());

        verify(spaceManagementService).findByHost(4L);
    }

    @Test
    void shouldCreateUpdateAndDeleteRoom() throws Exception {
        RoomDTO room = new RoomDTO();
        room.setRoomID(11L);
        room.setName("Lab");
        room.setCode("LB");

        when(hostRoomService.createRoomForHost(eq(4L), eq(7L), any())).thenReturn(room);
        when(hostRoomService.updateRoomForHost(eq(4L), eq(7L), eq(11L), any())).thenReturn(room);

        mockMvc.perform(post("/api/hosts/spaces/7/rooms")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lab","code":"LB"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/hosts/spaces/7/rooms/11")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lab","code":"LB"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/hosts/spaces/7/rooms/11")
                        .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isNoContent());

        verify(hostRoomService).deleteRoomForHost(4L, 7L, 11L);
    }
}
