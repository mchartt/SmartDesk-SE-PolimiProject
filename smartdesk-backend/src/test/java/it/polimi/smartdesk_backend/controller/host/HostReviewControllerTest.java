package it.polimi.smartdesk_backend.controller.host;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import it.polimi.smartdesk_backend.dto.review.ReviewResponseDTO;
import it.polimi.smartdesk_backend.exception.RestExceptionHandler;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.service.review.HostReviewService;
import it.polimi.smartdesk_backend.service.security.AccessControlService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.util.support.SecurityTestUtils;

@WebMvcTest(HostReviewController.class)
@Import(RestExceptionHandler.class)
class HostReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostReviewService hostReviewService;
    @MockitoBean
    private SpaceManagementService spaceManagementService;
    @MockitoBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.stubHostPathAccess(accessControlService, 4L, Role.HOST);
    }


    @Test
    void getMyReviews() throws Exception {
        ReviewResponseDTO review = new ReviewResponseDTO();
        review.setHostID(4L);
        when(hostReviewService.getReviewResponsesForHost(4L)).thenReturn(List.of(review));

        mockMvc.perform(get("/api/hosts/{hostID}/reviews", 4L)
                .principal(SecurityTestUtils.authenticatedUser(4L, Role.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostID").value(4));
    }
}
