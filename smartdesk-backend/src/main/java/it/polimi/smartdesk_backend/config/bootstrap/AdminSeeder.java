package it.polimi.smartdesk_backend.config.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.util.policy.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Garantisce almeno un {@link SysAdmin} in DB all'avvio se mancante, usando {@code app.seed.admin.email/password} (default in profilo {@code dev}, obbligatori altrove). */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwords;

    @Value("${app.seed.admin.email:}")
    private String adminSeedEmail;

    @Value("${app.seed.admin.password:}")
    private String adminSeedPassword;

    /** Idempotente: se esiste già un SysAdmin non crea duplicati. */
    @Override
    @Transactional
    public void run(String... args) {
        if (users.findAll().stream().anyMatch(SysAdmin.class::isInstance)) {
            return;
        }
        if (isBlank(adminSeedEmail) || isBlank(adminSeedPassword)) {
            log.warn("SysAdmin non creato: impostare app.seed.admin.email/password.");
            return;
        }
        if (!PasswordPolicy.isStrong(adminSeedPassword)) {
            throw new IllegalStateException("app.seed.admin.password non rispetta la policy password.");
        }
        String email = adminSeedEmail.trim();
        if (users.findByEmail(email).isPresent()) {
            log.warn("SysAdmin non creato: email {} gia registrata.", email);
            return;
        }

        SysAdmin admin = new SysAdmin();
        admin.setName("System Administrator");
        admin.setEmail(email);
        admin.setPassword(passwords.encode(adminSeedPassword.trim()));
        admin.setActive(true);

        users.save(admin);
        log.info("SysAdmin creato: {}", email);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

