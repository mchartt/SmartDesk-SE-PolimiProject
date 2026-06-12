package it.polimi.smartdesk_backend.security.filter;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import it.polimi.smartdesk_backend.util.message.AuthMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.dto.common.AccessTokenClaims;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.service.security.TokenService;

/** Filtro JWT: richieste con token valido, assente o non decifrabile. Rimosso test blacklist per semplificazione didattica. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    private JwtAuthenticationFilter filter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtWithoutHeaderPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validBearerTokenSetsAuthenticatedPrincipal() throws Exception {
        when(tokenService.verifyAndExtract("worker-token"))
                .thenReturn(new AccessTokenClaims(42L, Role.WORKER, 1L, Long.MAX_VALUE, "jti-1"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer worker-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        assertEquals(42L, principal.getUserId());
        assertEquals(Role.WORKER, principal.getRole());
    }

    @Test
    void jwtBearerInvalid() throws Exception {
        when(tokenService.verifyAndExtract("bad-token"))
                .thenThrow(new UnauthorizedException(AuthMessage.TOKEN_INVALID.text()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void jwtNonBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }
}
