package it.polimi.smartdesk_backend.config.web;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.security.filter.JwtAuthenticationFilter;
import it.polimi.smartdesk_backend.service.security.TokenService;

/** Questa classe configura la sicurezza HTTP dell'app: chi può chiamare le API, CORS per il frontend e login con JWT senza sessioni. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final List<String> DEV_ORIGIN_PATTERNS = List.of(
            "http://localhost:[*]",
            "https://localhost:[*]",
            "http://127.0.0.*:[*]",
            "https://127.0.0.*:[*]",
            "http://127.*.*.*:[*]",
            "https://127.*.*.*:[*]",
            "http://[::1]:[*]",
            "https://[::1]:[*]");

    @Value("${ALLOWED_ORIGINS:}")
    private String allowedOrigins;

    @Value("${CORS_ORIGIN:}")
    private String corsOrigin;

    /** Serve a dire al browser quali domini possono chiamare le API (legge ALLOWED_ORIGINS e CORS_ORIGIN). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(Environment environment) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(resolveAllowedOrigins(environment));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /** Serve a definire quali rotte sono pubbliche (auth, Swagger) e quali richiedono un token JWT. */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TokenService tokenService,
            ObjectMapper objectMapper) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/register/host",
                                "/api/auth/login",
                                "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtAuthenticationFilter(tokenService, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private List<String> resolveAllowedOrigins(Environment environment) {
        List<String> configured = parseCommaSeparated(allowedOrigins, corsOrigin);

        if (configured.isEmpty()) {
            return DEV_ORIGIN_PATTERNS;
        }

        if (isProd(environment)) {
            return configured;
        }

        var merged = new ArrayList<>(configured);
        merged.addAll(DEV_ORIGIN_PATTERNS);
        return merged.stream().distinct().toList();
    }

    private static List<String> parseCommaSeparated(String... values) {
        return Arrays.stream(String.join(",", values).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static boolean isProd(Environment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}

