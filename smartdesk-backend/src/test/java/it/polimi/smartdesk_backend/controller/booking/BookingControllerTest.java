package it.polimi.smartdesk_backend.controller.booking;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

/** Slice WebMvc su {@link BookingController}: dettaglio prenotazione e controlli di permessi. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@WebMvcTest(BookingController.class)
@Import(RestExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingQueryService bookingQueryService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void getBookingForAuthenticatedUserRole() throws Exception {
        BookingDTO booking = new BookingDTO(101L, 11L, LocalDateTime.of(2026, 4, 25, 9, 0), LocalDateTime.of(2026, 4, 25, 17, 0));
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(7L, Role.WORKER));
        when(bookingQueryService.findByIdForUser(101L, 7L, Role.WORKER)).thenReturn(booking);

        mockMvc.perform(get("/api/bookings/{bookingId}", 101L)
                .principal(SecurityTestUtils.authenticatedUser(7L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingID").value(101))
                .andExpect(jsonPath("$.deskID").value(11));

        verify(accessControlService).assertAuthenticated(any());
        verify(bookingQueryService).findByIdForUser(101L, 7L, Role.WORKER);
    }

    @Test
    void anonymousReadsBookingRejected() throws Exception {
        when(accessControlService.assertAuthenticated(any()))
                .thenThrow(new UnauthorizedException(AuthMessage.AUTHENTICATION_REQUIRED.text()));

        mockMvc.perform(get("/api/bookings/{bookingId}", 101L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonexistentBookingForWorker() throws Exception {
        when(accessControlService.assertAuthenticated(any()))
                .thenReturn(new AuthenticatedUser(42L, Role.WORKER));
        when(bookingQueryService.findByIdForUser(999L, 42L, Role.WORKER))
                .thenThrow(new NotFoundException(ResourceMessage.bookingNotFound(999L)));

        mockMvc.perform(get("/api/bookings/{bookingId}", 999L)
                .principal(SecurityTestUtils.authenticatedUser(42L, Role.WORKER)))
                .andExpect(status().isNotFound());
    }
}
