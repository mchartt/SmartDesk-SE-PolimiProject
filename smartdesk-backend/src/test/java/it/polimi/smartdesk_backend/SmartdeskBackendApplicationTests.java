package it.polimi.smartdesk_backend;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import it.polimi.smartdesk_backend.config.bootstrap.AdminSeeder;
import it.polimi.smartdesk_backend.config.bootstrap.DemoDataSeeder;
import it.polimi.smartdesk_backend.service.security.AuthService;
import it.polimi.smartdesk_backend.service.security.TokenService;

/** Verifica che il contesto Spring si avvii con il profilo {@code test} e che i bean fondamentali (seed admin, demo users, login, token) siano registrati. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@SpringBootTest
@ActiveProfiles("test")
class SmartdeskBackendApplicationTests {

    @Autowired
    private ApplicationContext context;

    /** Controlla che il contesto Spring non sia nullo dopo l'avvio. */
    @Test
    void springContextStartsWithTestProfile() {
        assertNotNull(context);
    }

    /** Verifica la presenza nel contesto dei componenti usati da login e seed. */
    @Test
    void authSeederAndTokenRegisteredInTheContext() {
        assertNotNull(context.getBean(AdminSeeder.class));
        assertNotNull(context.getBean(DemoDataSeeder.class));
        assertNotNull(context.getBean(AuthService.class));
        assertNotNull(context.getBean(TokenService.class));
    }
}
