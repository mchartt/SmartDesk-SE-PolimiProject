package it.polimi.smartdesk_backend.controller.admin;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.admin.LogDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.dto.auth.UserProfileDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.UserModerationAction;
import it.polimi.smartdesk_backend.service.booking.BookingCancellationService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.review.AdminReviewService;
import it.polimi.smartdesk_backend.service.admin.AdminSpaceOperations;
import it.polimi.smartdesk_backend.service.admin.SysAdminService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Operazioni da amministratore di sistema: utenti, spazi, log, moderazione, prenotazioni. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(SysAdminController.class)
@Import({RestExceptionHandler.class})
class SysAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysAdminService sysAdminService;

    @MockitoBean
    private AdminSpaceOperations adminSpaceOperations;

    @MockitoBean
    private AdminReviewService adminReviewService;

    @MockitoBean
    private BookingCancellationService bookingCancellationService;

    @MockitoBean
    private BookingQueryService bookingQueryService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void adminBansUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userID}", 9L)
                .param("action", UserModerationAction.BAN.name())
                .header("X-Forwarded-For", "10.0.0.7")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).moderateUser(isNull(), eq(9L), eq(UserModerationAction.BAN), eq("10.0.0.7"));
    }

    @Test
    void adminReactivatesUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userID}", 9L)
                .param("action", UserModerationAction.REACTIVATE.name())
                .header("X-Forwarded-For", "10.0.0.8")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).moderateUser(isNull(), eq(9L), eq(UserModerationAction.REACTIVATE), eq("10.0.0.8"));
    }

    @Test
    void moderationWrongAction() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userID}", 1L)
                .param("action", "SUSPEND")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminBanDoesNotCallReactivate() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userID}", 11L)
                .param("action", UserModerationAction.BAN.name())
                .header("X-Forwarded-For", "10.0.0.11")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).moderateUser(isNull(), eq(11L), eq(UserModerationAction.BAN), eq("10.0.0.11"));
        verify(sysAdminService, never())
                .moderateUser(anyLong(), anyLong(), eq(UserModerationAction.REACTIVATE), anyString());
    }

    @Test
    void adminListsUsers() throws Exception {
        UserProfileDTO user = new UserProfileDTO();
        user.setUserID(3L);
        user.setEmail("host@example.com");

        when(sysAdminService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userID").value(3));
    }

    @Test
    void adminClosesSpace() throws Exception {
        mockMvc.perform(delete("/api/admin/spaces/{spaceId}", 77L)
                .header("X-Forwarded-For", "10.0.0.9")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).forceCloseSpace(eq(77L), eq("10.0.0.9"));
    }

    @Test
    void adminAllSpaces() throws Exception {
        SpaceDTO space = new SpaceDTO();
        space.setSpaceID(21L);
        when(adminSpaceOperations.findAllForAdmin()).thenReturn(List.of(space));

        mockMvc.perform(get("/api/admin/spaces")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(21));
    }

    @Test
    void adminPendingSpaces() throws Exception {
        SpaceDTO space = new SpaceDTO();
        space.setSpaceID(22L);
        when(adminSpaceOperations.findPendingApprovalForAdmin()).thenReturn(List.of(space));

        mockMvc.perform(get("/api/admin/spaces/pending")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(22));
    }

    @Test
    void adminApprovedEnrichedSpaces() throws Exception {
        SpaceDTO space = new SpaceDTO();
        space.setSpaceID(31L);
        when(adminSpaceOperations.findApprovedEnrichedForAdmin()).thenReturn(List.of(space));

        mockMvc.perform(get("/api/admin/spaces/approved")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceID").value(31));

        verify(adminSpaceOperations).findApprovedEnrichedForAdmin();
    }

    @Test
    void adminHostApprovalCycle() throws Exception {
        UserProfileDTO pendingHost = new UserProfileDTO();
        pendingHost.setUserID(55L);
        pendingHost.setEmail("pending-host@example.com");

        when(sysAdminService.getPendingHosts()).thenReturn(List.of(pendingHost));

        mockMvc.perform(patch("/api/admin/hosts/{hostID}/approve", 55L)
                .header("X-Forwarded-For", "10.0.0.55")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());
        verify(sysAdminService).approveHost(eq(55L), eq("10.0.0.55"));

        mockMvc.perform(patch("/api/admin/hosts/{hostID}/reject", 56L)
                .header("X-Forwarded-For", "10.0.0.56")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());
        verify(sysAdminService).rejectHost(eq(56L), eq("10.0.0.56"));

        mockMvc.perform(get("/api/admin/hosts/pending")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userID").value(55))
                .andExpect(jsonPath("$[0].email").value("pending-host@example.com"));
    }

    @Test
    void adminApprovesSpace() throws Exception {
        mockMvc.perform(patch("/api/admin/spaces/{spaceId}/approve", 43L)
                .header("X-Forwarded-For", "10.0.0.43")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).approveSpace(eq(43L), eq("10.0.0.43"));
    }

    @Test
    void adminRejectsSpace() throws Exception {
        mockMvc.perform(patch("/api/admin/spaces/{spaceId}/reject", 44L)
                .header("X-Forwarded-For", "10.0.0.44")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(sysAdminService).rejectSpace(eq(44L), eq("10.0.0.44"));
    }

    @Test
    void adminListsBookings() throws Exception {
        BookingDTO booking = new BookingDTO(7L, 2L,
                LocalDateTime.of(2026, 5, 20, 9, 0),
                LocalDateTime.of(2026, 5, 20, 12, 0));
        booking.setBookingCode("ABC123");
        booking.setStatus("CONFIRMED");
        booking.setWorkerEmail("mario.rossi@worker.com");
        booking.setWorkerName("Mario Rossi");
        when(bookingQueryService.getAllBookingsForAdmin()).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/admin/bookings")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingID").value(7))
                .andExpect(jsonPath("$[0].bookingCode").value("ABC123"))
                .andExpect(jsonPath("$[0].workerEmail").value("mario.rossi@worker.com"))
                .andExpect(jsonPath("$[0].workerName").value("Mario Rossi"));
    }

    @Test
    void adminCancelsBooking() throws Exception {
        doNothing().when(bookingCancellationService).removeBookingForUser(7L, null, Role.SYS_ADMIN);

        mockMvc.perform(delete("/api/admin/bookings/{bookingId}", 7L)
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(bookingCancellationService).removeBookingForUser(eq(7L), isNull(), eq(Role.SYS_ADMIN));
    }

    @Test
    void adminSystemLog() throws Exception {
        LogDTO log = new LogDTO(1L, "SYS_ADMIN", "BAN_USER", LocalDateTime.of(2026, 4, 25, 8, 0), "WARNING", "127.0.0.1");
        when(sysAdminService.getSystemLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/logs")
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logID").value(1))
                .andExpect(jsonPath("$[0].severity").value("WARNING"));
    }

    @Test
    void adminDeleteReview() throws Exception {
        mockMvc.perform(delete("/api/admin/reviews/{reviewID}", 42L)
                .principal(SecurityTestUtils.authenticatedUser(1L, Role.SYS_ADMIN)))
                .andExpect(status().isNoContent());

        verify(adminReviewService).deleteReviewAsAdmin(42L);
    }
}
