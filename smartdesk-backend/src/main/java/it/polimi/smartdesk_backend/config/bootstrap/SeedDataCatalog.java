package it.polimi.smartdesk_backend.config.bootstrap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

/** Catalogo dei dati di seed caricati dal file JSON. Centralizza la lettura del file per evitare ricaricamenti multipli e race condition. */
@Slf4j
@Component
public class SeedDataCatalog {

    @Getter
    private final Optional<SeedData> data;

    /**
     * Tenta il caricamento da {@code classpath:seed-data.json}; empty se assente o illeggibile.
     *
     * @param json mapper condiviso per deserializzare il file
     */
    public SeedDataCatalog(ObjectMapper json) {
        this.data = loadSeedData(json);
    }

    private Optional<SeedData> loadSeedData(ObjectMapper json) {
        ClassPathResource resource = new ClassPathResource("seed-data.json");
        if (!resource.exists()) {
            log.info("File seed-data.json non trovato, il seeder verrà saltato.");
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            ObjectMapper seedMapper = json.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return Optional.of(seedMapper.readValue(in, SeedData.class));
        } catch (Exception ex) {
            log.warn("Impossibile caricare seed-data.json: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}

