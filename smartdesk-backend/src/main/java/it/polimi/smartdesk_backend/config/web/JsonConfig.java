package it.polimi.smartdesk_backend.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import lombok.NoArgsConstructor;

/** {@link ObjectMapper} condiviso: date/time Java 8 in ISO-8601, non timestamp numerici. */
@Configuration
@NoArgsConstructor
public class JsonConfig {

    /** Mapper condiviso dall'app (date ISO-8601, no timestamp numerici). */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

