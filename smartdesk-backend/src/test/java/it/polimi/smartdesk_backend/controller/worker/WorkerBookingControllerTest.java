package it.polimi.smartdesk_backend.controller.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.booking.BookingCancellationService;
import it.polimi.smartdesk_backend.service.booking.BookingCreationService;
import it.polimi.smartdesk_backend.service.booking.BookingEndSessionService;
import it.polimi.smartdesk_backend.service.booking.BookingQueryService;
import it.polimi.smartdesk_backend.service.booking.BookingWorkerHistoryService;
import it.polimi.smartdesk_backend.service.booking.DeskAvailabilityService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(WorkerBookingController.class)
@Import(RestExceptionHandler.class)
class WorkerBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingCreationService bookingCreationService;
    @MockitoBean
    private BookingCancellationService bookingCancellationService;
    @MockitoBean
    private BookingEndSessionService bookingEndSessionService;
    @MockitoBean
    private BookingQueryService bookingQueryService;
    @MockitoBean
    private BookingWorkerHistoryService bookingWorkerHistoryService;
    @MockitoBean
    private DeskAvailabilityService deskAvailabilityService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(4L, Role.WORKER));
    }

    @Test
    void workerCreatesBookingOk() throws Exception {
        BookingDTO booking = new BookingDTO(99L, 10L, LocalDateTime.of(2026, 4, 18, 9, 0), LocalDateTime.of(2026, 4, 18, 18, 0));
        when(bookingCreationService.createBooking(eq(4L), any())).thenReturn(booking);

        mockMvc.perform(post("/api/workers/bookings")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "deskID": 10,
                          "startTime": "2026-04-18T09:00:00",
                          "end": "2026-04-18T18:00:00"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingID").value(99));
    }

    @Test
    void workerMyBookings() throws Exception {
        BookingDTO booking = new BookingDTO(88L, 10L, LocalDateTime.of(2026, 4, 18, 9, 0), LocalDateTime.of(2026, 4, 18, 12, 0));
        when(bookingQueryService.getBookingsByWorker(4L)).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/workers/bookings")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingID").value(88));
    }

    @Test
    void workerMovesBooking() throws Exception {
        BookingDTO booking = new BookingDTO(99L, 10L, LocalDateTime.of(2030, 4, 19, 9, 0), LocalDateTime.of(2030, 4, 19, 18, 0));
        when(bookingCreationService.rescheduleBooking(eq(4L), eq(99L), any())).thenReturn(booking);

        mockMvc.perform(patch("/api/workers/bookings/{bookingId}", 99L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "bookingId": 99,
                          "version": 7,
                          "newStart": "2030-04-19T09:00:00",
                          "newEnd": "2030-04-19T18:00:00"
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void workerCancelsBooking() throws Exception {
        mockMvc.perform(delete("/api/workers/bookings/{bookingId}", 99L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isNoContent());

        verify(bookingCancellationService).removeBookingForUser(99L, 4L, Role.WORKER);
    }

    @Test
    void workerLeavesDesk() throws Exception {
        BookingDTO booking = new BookingDTO(99L, 10L, LocalDateTime.of(2026, 5, 21, 16, 0), LocalDateTime.of(2026, 5, 21, 16, 45));
        when(bookingEndSessionService.endSessionForWorker(4L, 99L)).thenReturn(booking);

        mockMvc.perform(post("/api/workers/bookings/{bookingId}/leave", 99L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingID").value(99))
                .andExpect(jsonPath("$.endTime").exists());

        verify(bookingEndSessionService).endSessionForWorker(4L, 99L);
    }

    @Test
    void workerClearsBookingsHistory() throws Exception {
        when(bookingWorkerHistoryService.clearPastBookingsForWorker(4L)).thenReturn(3);

        mockMvc.perform(delete("/api/workers/bookings/history")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(3));
    }

    @Test
    void workerSearchesDeskOk() throws Exception {
        List<DeskDTO> desks = List.of(new DeskDTO(10L, "A1", "Building A", List.of("wifi", "monitor")));
        when(deskAvailabilityService.searchDesks(any())).thenReturn(desks);

        mockMvc.perform(post("/api/workers/bookings/search")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "targetDate": "2026-04-18",
                          "requiredAmenities": ["wifi", "monitor"]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }
}
