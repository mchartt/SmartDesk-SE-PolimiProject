package it.polimi.smartdesk_backend.repository.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.user.RefreshToken;

/** Sessioni “lunghe” per il refresh JWT: lookup per valore token e revoca massiva per utente. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(UUID token);

    /** Lookup refresh token per ID utente senza caricare l'entità {@link it.polimi.smartdesk_backend.model.user.User}. */
    List<RefreshToken> findByUser_IdAndRevoked(Long userId, boolean revoked);
}

