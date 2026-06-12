package it.polimi.smartdesk_backend.controller.auth;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.auth.AuthResponseDTO;
import it.polimi.smartdesk_backend.dto.auth.RefreshTokenRequestDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AccessTokenClaims;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.security.AuthService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Slice WebMvc su {@link AuthController}: registrazione, login, refresh, logout e profilo corrente. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(AuthController.class)
@Import(RestExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(7L, Role.WORKER));
        when(tokenService.verifyAndExtract("logout-token"))
                .thenReturn(new AccessTokenClaims(7L, Role.WORKER, 1L, Long.MAX_VALUE, "logout-jti"));
    }

    @Test
    void registrationWorkerOk() throws Exception {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken("access-token");
        response.setRefreshToken(UUID.randomUUID().toString());
        response.setTokenType("Bearer");
        response.setExpiresIn(Instant.parse("2026-04-25T10:00:00Z"));

        when(authService.register(any(), eq("10.0.0.1"))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .header("X-Forwarded-For", "10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Mario",
                          "surname": "Rossi",
                          "email": "mario@example.com",
                          "password": "StrongPass123!",
                          "role": "WORKER",
                          "company": "ACME"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginEmptyFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "",
                          "password": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerHostRefreshProfile() throws Exception {
        AuthResponseDTO hostRegister = new AuthResponseDTO();
        hostRegister.setUserID(17L);
        hostRegister.setRole("HOST");

        AuthResponseDTO refreshed = new AuthResponseDTO();
        refreshed.setAccessToken("refreshed-access-token");
        refreshed.setRefreshToken(UUID.randomUUID().toString());
        refreshed.setTokenType("Bearer");
        refreshed.setExpiresIn(Instant.parse("2026-04-25T10:10:00Z"));
        refreshed.setUserID(17L);

        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserID(17L);
        profile.setEmail("host@example.com");
        profile.setRole("HOST");

        when(authService.registerHost(any(), eq("10.0.0.3"))).thenReturn(hostRegister);
        when(authService.refresh(any(RefreshTokenRequestDTO.class), eq("10.0.0.4"))).thenReturn(refreshed);
        when(authService.getUserProfileForRequester(17L, 7L)).thenReturn(profile);

        mockMvc.perform(post("/api/auth/register/host")
                .header("X-Forwarded-For", "10.0.0.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Host",
                          "surname": "One",
                          "email": "host@example.com",
                          "password": "StrongPass123!",
                          "nameStructure": "HQ Milan",
                          "vatNumber": "VAT123456",
                          "description": "Coworking location in city center with flexible desks, meeting rooms and full services for daily teams"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userID").value(17))
                .andExpect(jsonPath("$.role").value("HOST"));

        mockMvc.perform(post("/api/auth/refresh")
                .header("X-Forwarded-For", "10.0.0.4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "refreshToken": "123e4567-e89b-12d3-a456-426614174000"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("refreshed-access-token"));

        mockMvc.perform(get("/api/auth/users/{userID}/profile", 17L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").value(17))
                .andExpect(jsonPath("$.role").value("HOST"));
    }

    @Test
    void currentProfileAliasForFrontend() throws Exception {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserID(7L);
        profile.setEmail("worker@example.com");
        profile.setRole("WORKER");

        when(authService.getUserProfileForRequester(7L, 7L)).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").value(7))
                .andExpect(jsonPath("$.role").value("WORKER"));
    }

    @Test
    void logoutRequester() throws Exception {
        mockMvc.perform(delete("/api/auth/logout/{userID}", 5L)
                .header("Authorization", "Bearer logout-token")
                .header("X-Forwarded-For", "10.0.0.2")
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(accessControlService).assertAuthenticated(any());
        verify(authService).logoutForRequester(5L, 7L, "10.0.0.2");
    }

    @Test
    void loginBrokenJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@test.it\","))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutWithoutHeaderCallsService() throws Exception {
        mockMvc.perform(delete("/api/auth/logout/{userID}", 5L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(authService).logoutForRequester(5L, 7L, "127.0.0.1");
    }
}
