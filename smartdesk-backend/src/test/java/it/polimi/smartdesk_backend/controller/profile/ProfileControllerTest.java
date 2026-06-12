package it.polimi.smartdesk_backend.controller.profile;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.dto.notification.NotificationDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.notification.NotificationQueryService;
import it.polimi.smartdesk_backend.service.notification.NotificationStreamHub;
import it.polimi.smartdesk_backend.service.profile.ProfileService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Profilo utente, notifiche e preferenze esposte da {@link ProfileController}. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(ProfileController.class)
@Import(RestExceptionHandler.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private NotificationQueryService notificationQueryService;

    @MockitoBean
    private NotificationStreamHub notificationStreamHub;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(7L, Role.WORKER));
    }

    @Test
    void readProfile() throws Exception {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserID(7L);
        profile.setName("Mario");
        when(profileService.getProfile(7L)).thenReturn(profile);

        mockMvc.perform(get("/api/profile")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mario"));
    }

    @Test
    void profilePasswordDeleteCycle() throws Exception {
        UserProfileDTO updated = new UserProfileDTO();
        updated.setUserID(7L);
        updated.setName("Luigi");
        updated.setSurname("Verdi");
        updated.setEmail("luigi@test.it");
        when(profileService.updateProfile(7L, "Luigi", "Verdi", "luigi@test.it")).thenReturn(updated);

        mockMvc.perform(put("/api/profile")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Luigi","surname":"Verdi","email":"luigi@test.it"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luigi"));

        mockMvc.perform(put("/api/profile/password")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"old","newPassword":"new"}
                        """))
                .andExpect(status().isNoContent());

        verify(profileService).changePassword(7L, "old", "new");

        mockMvc.perform(delete("/api/profile")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .header("X-Forwarded-For", "10.0.0.12"))
                .andExpect(status().isNoContent());

        verify(profileService).deleteAccount(7L, "10.0.0.12");
    }

    @Test
    void listNotifications() throws Exception {
        NotificationDTO n = new NotificationDTO();
        n.setNotificationID(1L);
        n.setMessage("Hello");
        when(notificationQueryService.listForRecipient(eq(7L)))
                .thenReturn(List.of(n));

        mockMvc.perform(get("/api/profile/notifications")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationID").value(1))
                .andExpect(jsonPath("$[0].message").value("Hello"));
    }

    @Test
    void markNotificationRead() throws Exception {
        mockMvc.perform(patch("/api/profile/notifications/{id}/read", 55L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(notificationQueryService).markAsRead(55L, 7L);
    }

    @Test
    void markAllRead() throws Exception {
        mockMvc.perform(patch("/api/profile/notifications/read-all")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(notificationQueryService).markAllAsRead(7L);
    }

    @Test
    void clearReadHistory() throws Exception {
        mockMvc.perform(delete("/api/profile/notifications/history")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(notificationQueryService).clearReadHistory(7L);
    }

    @Test
    void unreadCount() throws Exception {
        when(notificationQueryService.unreadCount(7L)).thenReturn(3L);

        mockMvc.perform(get("/api/profile/notifications/unread-count")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    void emptyNameValidation() throws Exception {
        mockMvc.perform(put("/api/profile")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","surname":"X","email":"a@b.it"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changePasswordMissingParam() throws Exception {
        mockMvc.perform(put("/api/profile/password")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"old"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void junkPayload() throws Exception {
        mockMvc.perform(put("/api/profile")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Luigi\""))
                .andExpect(status().isBadRequest());
    }
}
