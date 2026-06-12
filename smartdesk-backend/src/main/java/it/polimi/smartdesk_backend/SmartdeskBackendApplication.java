package it.polimi.smartdesk_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.NoArgsConstructor;

/** Entry point Spring Boot SmartDesk (scheduling abilitato a livello contesto). */
@SpringBootApplication
@EnableScheduling
@NoArgsConstructor
public class SmartdeskBackendApplication {

	/**
	 * Avvia il contesto Spring e l'embedded server.
	 *
	 * @param args argomenti da riga di comando (passati a Spring Boot)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SmartdeskBackendApplication.class, args);
	}

}

