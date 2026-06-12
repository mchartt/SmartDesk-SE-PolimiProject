package it.polimi.smartdesk_backend.config.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Se app.seed.test-users.enabled è true, carica il dataset demo nell'ordine: utenti, spazi, booking, review, ticket. In prod di solito resta spento. */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final SeedDataCatalog seedDataCatalog;
    private final UserSeeder userSeeder;
    private final SpaceSeeder spaceSeeder;
    private final BookingSeeder bookingSeeder;
    private final ReviewSeeder reviewSeeder;
    private final TicketSeeder ticketSeeder;

    @Value("${app.seed.test-users.enabled:false}")
    private boolean enabled;

    /** Carica il dataset demo se {@code app.seed.test-users.enabled=true}. */
    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;
        seedDataCatalog.getData().ifPresent(data -> {
            userSeeder.seedWorkers(data.getWorkers());
            userSeeder.seedHosts(data.getHosts());
            userSeeder.seedTechnicians(data.getTechnicians());
            spaceSeeder.seedSpaces(data.getSpaces());
            userSeeder.seedTechnicianHostLinks(data.getTechnicians());
            bookingSeeder.seedBookings(data.getBookings());
            bookingSeeder.seedRecentCompletedBookingsForReviews();
            reviewSeeder.seedReviews(data.getReviews());
            ticketSeeder.seedTickets(data.getTickets());
        });
    }
}
