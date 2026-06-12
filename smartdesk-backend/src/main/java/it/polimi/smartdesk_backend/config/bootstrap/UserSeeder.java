package it.polimi.smartdesk_backend.config.bootstrap;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.util.policy.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Popola worker/host/tecnici dal JSON demo — salta le email già presenti. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder {

    private final UserRepository users;
    private final SpaceRepository spaces;
    private final PasswordEncoder passwords;

    /** Inserisce worker dal JSON demo; salta email già registrate. */
    public void seedWorkers(List<SeedData.WorkerJson> rows) {
        if (rows == null) return;
        for (SeedData.WorkerJson row : rows) {
            if (users.findByEmail(row.getEmail()).isEmpty()) {
                assertStrongSeedPassword(row.getEmail(), row.getPassword());
                Worker worker = new Worker();
                worker.setName(row.getName());
                worker.setSurname(row.getSurname());
                worker.setEmail(row.getEmail());
                worker.setPassword(passwords.encode(row.getPassword()));
                worker.setCompany(row.getCompany());
                worker.setActive(row.isActive());
                users.save(worker);
                log.info("Worker creato: {}", row.getEmail());
            }
        }
    }

    /** Inserisce host dal JSON demo; salta email già registrate. */
    public void seedHosts(List<SeedData.HostJson> rows) {
        if (rows == null) return;
        for (SeedData.HostJson row : rows) {
            if (users.findByEmail(row.getEmail()).isEmpty()) {
                assertStrongSeedPassword(row.getEmail(), row.getPassword());
                Host host = new Host();
                host.setName(row.getName());
                host.setSurname(row.getSurname());
                host.setEmail(row.getEmail());
                host.setPassword(passwords.encode(row.getPassword()));
                host.setNameStructure(row.getNameStructure());
                host.setVatNumber(row.getVatNumber());
                host.setApproved(row.isApproved());
                host.setActive(row.isActive());
                users.save(host);
                log.info("Host creato: {}", row.getEmail());
            }
        }
    }

    /** Inserisce tecnici dal JSON demo e collega l'host creatore se presente. */
    public void seedTechnicians(List<SeedData.TechnicianJson> rows) {
        if (rows == null) return;
        for (SeedData.TechnicianJson row : rows) {
            if (users.findByEmail(row.getEmail()).isEmpty()) {
                assertStrongSeedPassword(row.getEmail(), row.getPassword());
                Technician tech = new Technician();
                tech.setName(row.getName());
                tech.setSurname(row.getSurname());
                tech.setEmail(row.getEmail());
                tech.setPassword(passwords.encode(row.getPassword()));
                tech.setSpecialization(row.getSpecialisation());
                tech.setActive(row.isActive());
                resolveHostForTechnician(row).ifPresent(host -> tech.setCreatingHostId(host.getId()));
                users.save(tech);
                log.info("Tecnico creato: {}", row.getEmail());
            }
        }
    }

    /** Secondo passaggio: assegna agli spazi i tecnici già creati (dopo il seed sedi). */
    public void seedTechnicianHostLinks(List<SeedData.TechnicianJson> rows) {
        if (rows == null) return;
        for (SeedData.TechnicianJson row : rows) {
            if (row.getHostEmail() == null || row.getHostEmail().isBlank()) continue;
            Optional<Host> host = resolveHostForTechnician(row);
            if (host.isEmpty()) continue;
            var user = users.findByEmail(row.getEmail());
            if (user.isEmpty() || !(user.get() instanceof Technician technician)) continue;
            
            if (technician.getCreatingHostId() == null) {
                technician.setCreatingHostId(host.get().getId());
            }
            
            List<String> spaceNames = row.getAssignedSpaceNames();
            if (spaceNames == null || spaceNames.isEmpty()) {
                spaceNames = spaces.findByHostID(host.get().getId()).stream().map(Space::getName).toList();
            }
            
            for (String spaceName : spaceNames) {
                spaces.findAll().stream()
                        .filter(s -> s.getName().equals(spaceName) && host.get().getId().equals(s.getHostID()))
                        .findFirst()
                        .ifPresent(space -> {
                            if (technician.getSpaces().stream().noneMatch(s -> s.getSpaceID().equals(space.getSpaceID()))) {
                                technician.assignSpace(space);
                            }
                        });
            }
            users.save(technician);
        }
    }

    private Optional<Host> resolveHostForTechnician(SeedData.TechnicianJson row) {
        return users.findByEmail(row.getHostEmail().trim())
                .filter(Host.class::isInstance)
                .map(Host.class::cast);
    }

    private void assertStrongSeedPassword(String email, String password) {
        if (!PasswordPolicy.isStrong(password)) {
            throw new IllegalStateException("Password seed debole per " + email + ".");
        }
    }
}
