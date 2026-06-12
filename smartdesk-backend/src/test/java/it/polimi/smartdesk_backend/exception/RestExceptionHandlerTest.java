package it.polimi.smartdesk_backend.exception;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.polimi.smartdesk_backend.util.support.BodyRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/** {@link RestExceptionHandler} provato con MockMvc standalone e controller fittizio. */
@FieldDefaults(level = AccessLevel.PRIVATE)
class RestExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    @Test
    void clientErrors() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));

        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));

        mockMvc.perform(get("/test/path/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("value"));

        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void forbiddenAndUnauthorized() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/test/authorization-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void notFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void conflicts() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(get("/test/optimistic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK"));

        mockMvc.perform(get("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    void wrongMethodAndInternal() throws Exception {
        mockMvc.perform(get("/test/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @RestController
    @Validated
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/header")
        public String requireHeader(@RequestHeader("X-Test") String header) {
            return header;
        }

        @GetMapping("/param")
        public String requireParam(@RequestParam("required") String required) {
            return required;
        }

        @GetMapping("/path/{id}")
        public Long requireLongPath(@PathVariable Long id) {
            return id;
        }

        @PostMapping("/body")
        public String requireBody(@Valid @RequestBody BodyRequest request) {
            return request.value();
        }

        @GetMapping("/business")
        public String businessRule() {
            throw new BusinessRuleException("Rule");
        }

        @GetMapping("/not-found")
        public String notFound() {
            throw new NotFoundException("Missing");
        }

        @GetMapping("/forbidden")
        public String forbidden() {
            throw new ForbiddenException("Denied");
        }

        @GetMapping("/authorization-denied")
        public String authorizationDenied() {
            throw new AuthorizationDeniedException("Denied");
        }

        @GetMapping("/unauthorized")
        public String unauthorized() {
            throw new UnauthorizedException("No token");
        }

        @GetMapping("/constraint")
        public String constraintViolation() {
            throw new ConstraintViolationException("constraint failed", Collections.emptySet());
        }

        @GetMapping("/integrity")
        public String integrityViolation() {
            throw new DataIntegrityViolationException("Duplicate");
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new RuntimeException("Boom");
        }

        @GetMapping("/conflict")
        public String conflict() {
            throw new ConflictException("Stale version");
        }

        @GetMapping("/optimistic")
        public String optimistic() {
            throw new ObjectOptimisticLockingFailureException(Object.class, "stale");
        }
    }

}
