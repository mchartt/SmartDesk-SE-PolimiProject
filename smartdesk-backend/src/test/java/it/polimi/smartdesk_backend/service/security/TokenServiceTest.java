package it.polimi.smartdesk_backend.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.smartdesk_backend.dto.common.AccessTokenClaims;
import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.util.support.EntityTestFixtures;

class TokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(SECRET, "smartdesk-test", 900L);
    }

    @Test
    void shouldGenerateAndVerifyAccessToken() {
        var worker = EntityTestFixtures.worker(42L);

        String token = tokenService.generateAccessToken(worker);
        AccessTokenClaims claims = tokenService.verifyAndExtract(token);

        assertEquals(42L, claims.getUserId());
        assertEquals(Role.WORKER, claims.getRole());
        assertNotNull(claims.getTokenId());
    }

    @Test
    void shouldExposeAccessTokenExpiry() {
        Instant expiry = tokenService.getAccessTokenExpiry();
        assertNotNull(expiry);
    }

    @Test
    void shouldRejectMissingToken() {
        assertThrows(UnauthorizedException.class, () -> tokenService.verifyAndExtract(null));
        assertThrows(UnauthorizedException.class, () -> tokenService.verifyAndExtract("   "));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThrows(UnauthorizedException.class, () -> tokenService.verifyAndExtract("not.a.jwt"));
    }

    @Test
    void shouldRejectShortSecretAtConstruction() {
        assertThrows(IllegalStateException.class,
                () -> new TokenService("short", "issuer", 60L));
    }
}
