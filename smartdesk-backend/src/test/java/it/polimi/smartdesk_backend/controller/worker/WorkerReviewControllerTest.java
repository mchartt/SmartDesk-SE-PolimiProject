package it.polimi.smartdesk_backend.controller.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

import it.polimi.smartdesk_backend.dto.review.WorkerReviewHistoryDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.review.WorkerReviewService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(WorkerReviewController.class)
@Import(RestExceptionHandler.class)
class WorkerReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkerReviewService workerReviewService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        when(accessControlService.assertAuthenticated(any())).thenReturn(new AuthenticatedUser(4L, Role.WORKER));
    }

    @Test
    void shouldCreateWorkerReview() throws Exception {
        WorkerReviewHistoryDTO response = new WorkerReviewHistoryDTO();
        response.setWorkerID(4L);
        when(workerReviewService.leaveReviewForWorker(eq(4L), any())).thenReturn(response);

        mockMvc.perform(post("/api/workers/reviews")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "bookingID": 42,
                          "hostID": 2,
                          "rating": 5,
                          "spaceID": 7,
                          "comment": "Esperienza molto positiva, servizio rapido, ambiente pulito e personale sempre disponibile."
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workerID").value(4));
    }

    @Test
    void shouldListWorkerReviewHistory() throws Exception {
        WorkerReviewHistoryDTO row = new WorkerReviewHistoryDTO();
        row.setReviewID(1L);
        when(workerReviewService.getWorkerReviewHistory(4L)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/workers/reviews/history")
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewID").value(1));
    }
}
