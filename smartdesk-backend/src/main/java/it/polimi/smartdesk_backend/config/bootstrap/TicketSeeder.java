package it.polimi.smartdesk_backend.config.bootstrap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Ultimo step del seed: ticket in vari stati per provare il flusso. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketSeeder {

    private final UserRepository users;
    private final SpaceRepository spaces;
    private final DeskRepository desks;
    private final TicketRepository tickets;

    /** Ticket demo in vari stati per provare il flusso assistenza. */
    public void seedTickets(List<SeedData.TicketJson> rows) {
        if (rows == null) return;
        for (SeedData.TicketJson row : rows) {
            var worker = users.findByEmail(row.getWorkerEmail());
            Optional<Space> space = spaces.findAll().stream().filter(s -> s.getName().equals(row.getSpaceName())).findFirst();
            if (worker.isEmpty() || space.isEmpty()) continue;

            Desk desk = desks.findBySpace_SpaceIDAndCode(space.get().getSpaceID(), row.getDeskCode()).orElse(null);
            if (desk == null) continue;

            String code = row.getTicketCode() != null ? row.getTicketCode() : "TSEED" + desk.getDeskID();
            if (tickets.existsBySpaceIDAndTicketCode(space.get().getSpaceID(), code)) continue;

            Ticket ticket = new Ticket();
            ticket.report(desk);
            ticket.setWorkerID(worker.get().getId());
            ticket.setTitle(row.getTitle() != null && !row.getTitle().isBlank() ? row.getTitle() : "Segnalazione");
            ticket.setDescription(row.getDescription());
            ticket.setStatus(row.getStatus() != null ? row.getStatus() : "OPEN");
            ticket.setSeverity(row.getSeverity() != null ? row.getSeverity() : "MEDIUM");
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setTicketCode(code);
            ticket.setTechnicianNote(row.getTechnicianNote());
            if (row.getTechnicianEmail() != null) {
                users.findByEmail(row.getTechnicianEmail()).ifPresent(tech -> ticket.setTechnicianID(tech.getId()));
            }
            if ("RESOLVED".equalsIgnoreCase(row.getStatus())) {
                ticket.setResolution(row.getResolution() != null ? row.getResolution() : "Risoluzione seed");
                ticket.setResolvedAt(LocalDateTime.now().minusDays(1));
            }
            tickets.save(ticket);
            log.info("Ticket creato da: {}", row.getWorkerEmail());
        }
    }
}
