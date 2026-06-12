package it.polimi.smartdesk_backend.service.security;

import it.polimi.smartdesk_backend.util.message.AuthMessage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import it.polimi.smartdesk_backend.exception.UnauthorizedException;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.dto.common.AccessTokenClaims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** Token JWT access (HS256 via Nimbus). La revoca dei token tramite blacklist è disabilitata per semplicità didattica. */
@Service
@Slf4j
public class TokenService {

    private static final String ROLE_CLAIM = "role";

    private final String secret;
    private final String issuer;
    private final long ttlSeconds;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    /** Secret obbligatorio in avvio, minimo 32 caratteri — altrimenti Spring non parte. */
    public TokenService(
            @Value("${security.access-token-secret:}") String accessTokenSecret,
            @Value("${security.jwt-issuer:smartdesk-backend}") String jwtIssuer,
            @Value("${security.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.secret = accessTokenSecret;
        this.issuer = jwtIssuer;
        this.ttlSeconds = accessTokenTtlSeconds;
        assertSecretOk();
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Genera un nuovo access token JWT (sub, role, jti random). */
    public String generateAccessToken(User user) {
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claimsFor(user, Instant.now())))
                .getTokenValue();
    }

    /** Utile in login: quando scadrebbe un token emesso adesso. */
    public Instant getAccessTokenExpiry() {
        return Instant.now().plusSeconds(ttlSeconds);
    }

    /** Valida firma e claim; se manca qualcosa → UnauthorizedException con messaggio volutamente generico verso il client. */
    @Transactional(readOnly = true)
    public AccessTokenClaims verifyAndExtract(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException(AuthMessage.TOKEN_MISSING.text());
        }
        try {
            Jwt jwt = decoder.decode(token);
            if (!issuer.equals(jwt.getClaimAsString("iss"))) {
                throw new UnauthorizedException(AuthMessage.TOKEN_INVALID_ISSUER.text());
            }
            Long userId = parseUserId(jwt.getClaimAsString("sub"));
            Role role = parseRole(jwt.getClaimAsString(ROLE_CLAIM));
            Instant issuedAt = jwt.getClaimAsInstant("iat");
            Instant expiresAt = jwt.getClaimAsInstant("exp");
            String jti = jwt.getClaimAsString("jti");
            if (issuedAt == null || expiresAt == null || jti == null || jti.isBlank()) {
                throw new UnauthorizedException(AuthMessage.TOKEN_INVALID_PAYLOAD.text());
            }
            return new AccessTokenClaims(
                    userId, role, issuedAt.getEpochSecond(), expiresAt.getEpochSecond(), jti);
        } catch (BadJwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(AuthMessage.TOKEN_INVALID.text());
        }
    }

    /** Costruzione standard OAuth2: {@code jti} UUID generato per ogni token. */
    private JwtClaimsSet claimsFor(User user, Instant issuedAt) {
        return JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(ttlSeconds))
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();
    }

    /** {@code sub} deve essere intero decimale; qualsiasi altro formato → payload invalido. */
    private long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException(AuthMessage.TOKEN_INVALID_PAYLOAD.text());
        }
    }

    /** Il claim role deve combaciare con l'enum Role. */
    private Role parseRole(String roleValue) {
        try {
            return Role.valueOf(roleValue);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthorizedException(AuthMessage.TOKEN_INVALID_PAYLOAD.text());
        }
    }

    /** Vincolo allineato al messaggio di startup Spring quando manca {@code SECURITY_ACCESS_TOKEN_SECRET}. */
    private void assertSecretOk() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                    "Il segreto del token di accesso deve essere configurato con un valore complesso di almeno 32 caratteri.");
        }
    }
}
