package it.polimi.smartdesk_backend.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.NoArgsConstructor;

/** Bean security trasversali: oggi solo {@link PasswordEncoder} BCrypt usato da registrazione e seed. */
@Configuration
@NoArgsConstructor
public class SecurityBeansConfig {

    /** Hash password utenti (cost factor BCrypt di default). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

