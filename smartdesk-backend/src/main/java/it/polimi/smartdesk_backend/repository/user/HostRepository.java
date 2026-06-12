package it.polimi.smartdesk_backend.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import it.polimi.smartdesk_backend.model.user.Host;

/** Repository JPA per {@link it.polimi.smartdesk_backend.model.user.Host} con flag di approvazione. */
public interface HostRepository extends JpaRepository<Host, Long> {
}

