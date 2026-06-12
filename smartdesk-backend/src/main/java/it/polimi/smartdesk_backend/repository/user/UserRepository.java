package it.polimi.smartdesk_backend.repository.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.user.User;

/** Utenti polimorfici (worker/host/technician/sysadmin): lookup email e elenco admin. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("SELECT a FROM SysAdmin a")
    List<SysAdmin> findAllSysAdmins();
}

