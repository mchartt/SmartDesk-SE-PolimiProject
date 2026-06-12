package it.polimi.smartdesk_backend.security.filter;

import it.polimi.smartdesk_backend.util.message.AuthMessage;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.dto.common.AccessTokenClaims;
import it.polimi.smartdesk_backend.service.security.TokenService;
import it.polimi.smartdesk_backend.dto.common.AuthenticatedUser;
import it.polimi.smartdesk_backend.support.ApiErrorSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/** Filtro stateless JWT: verifica Bearer token, popola {@link AuthenticatedUser} o restituisce 401; assenza header consente accesso anonimo agli endpoint pubblici. */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokens;
    private final ObjectMapper objectMapper;
    /**
     * Token assente: pass-through; token presente ma invalido o revocato: 401 e interruzione catena.
     *
     * @param request richiesta HTTP in ingresso
     * @param response risposta su cui scrivere eventuale errore
     * @param chain filtri successivi
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ")) {
            reject(request, response, AuthMessage.TOKEN_MISSING.text());
            return;
        }
        String raw = authorization.substring(7).trim();
        if (raw.isBlank()) {
            reject(request, response, AuthMessage.TOKEN_MISSING.text());
            return;
        }

        try {
            AccessTokenClaims claims = tokens.verifyAndExtract(raw);
            AuthenticatedUser principal = new AuthenticatedUser(claims.getUserId(), claims.getRole());
            var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            SecurityContextHolder.clearContext();
            reject(request, response, ex.getMessage());
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT validation failed", ex);
            reject(request, response, AuthMessage.TOKEN_INVALID_OR_EXPIRED.text());
        }
    }

    /** Risposta JSON coerente con {@code RestExceptionHandler}. */
    private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        String resolved = message == null ? AuthMessage.UNAUTHORIZED_FALLBACK.text() : message;
        ApiErrorSupport.writeErrorResponse(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", resolved);
    }
}
