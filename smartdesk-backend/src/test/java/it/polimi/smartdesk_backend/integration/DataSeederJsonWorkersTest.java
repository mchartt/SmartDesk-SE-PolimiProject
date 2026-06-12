package it.polimi.smartdesk_backend.integration;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.user.UserRepository;

/** Con seed JSON attivo, i worker attesi vengono creati nel database H2 in memoria. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "app.seed.test-users.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:dataseeder_json_workers;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class DataSeederJsonWorkersTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedJsonCreatesKnownWorkers() {
        long workers = userRepository.findAll().stream().filter(Worker.class::isInstance).count();
        assertTrue(workers >= 2, "seed-data.json should create at least two workers");
        assertTrue(userRepository.findByEmail("mario.rossi@worker.com").isPresent());
    }
}
