package it.polimi.smartdesk_backend.integration;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import it.polimi.smartdesk_backend.config.bootstrap.DemoDataSeeder;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;

/** Il seed ticket da JSON non viola vincoli univoci se il seeder viene eseguito più volte (es. riavvio H2 su file). */
@FieldDefaults(level = AccessLevel.PRIVATE)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "app.seed.test-users.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:dataseeder_ticket_idempotency;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class DataSeederTicketIdempotencyTest {

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void seederTicketDoesNotDuplicate() {
        long afterStartup = ticketRepository.count();
        assertTrue(afterStartup >= 2, "seed-data.json defines two tickets");

        demoDataSeeder.run();
        entityManager.flush();
        assertEquals(afterStartup, ticketRepository.count(), "second full seed must not insert duplicate tickets");

        demoDataSeeder.run();
        entityManager.flush();
        assertEquals(afterStartup, ticketRepository.count(), "third full seed must not insert duplicate tickets");
    }
}
