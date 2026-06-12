package it.polimi.smartdesk_backend.config.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.NoArgsConstructor;

/** OpenAPI 3 con meta SmartDesk e schema security {@code bearerAuth} per provare gli endpoint protetti da Swagger UI. */
@Configuration
@NoArgsConstructor
public class SwaggerConfig {

    /** Configura OpenAPI e il pulsante Authorize JWT nella UI Swagger. */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("SmartDesk API")
                        .version("1.0")
                        .description("Documentazione delle API REST per la piattaforma SmartDesk")
                        .contact(new Contact().name("Team Cesandri & Gheojan")))
                // Configura il bottone per inserire il JWT
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
