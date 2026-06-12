package it.polimi.smartdesk_backend.mapper;

import java.util.List;
import java.util.Map;

import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;

import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.dto.ticket.TicketNoteMessageDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.dto.common.SpaceSummary;
import lombok.RequiredArgsConstructor;

/** Composizione di {@link TicketResponseDTO} da dati già risolti dal service (nessuna query nel mapper). */
@Component
@RequiredArgsConstructor
public class TicketResponseMapper {

    /** Compone il DTO ticket da entità e mappe precaricate. */
    public TicketResponseDTO buildResponseDTO(
            Ticket ticket,
            Map<Long, String> deskCodeByDeskId,
            Map<Long, SpaceSummary> spaceById,
            Map<Long, User> usersById,
            Map<Long, List<TicketTechnicianNote>> techNotesByTicketId,
            Map<Long, List<TicketWorkerNote>> workerNotesByTicketId,
            Map<Long, List<TicketHostNote>> hostNotesByTicketId) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setTicketID(ticket.getTicketID());
        dto.setTicketCode(ticket.getTicketCode());
        dto.setTitle(ticket.getTitle());
        dto.setStatus(ticket.getStatus());
        dto.setAssignedTechID(ticket.getTechnicianID());
        dto.setResolution(ticket.getResolution());
        dto.setDescription(ticket.getDescription());
        dto.setTechnicianNote(ticket.getTechnicianNote());
        dto.setTechnicianNoteHistory(buildTechnicianNoteHistory(ticket, techNotesByTicketId, usersById));
        dto.setWorkerNoteHistory(buildWorkerNoteHistory(ticket, workerNotesByTicketId, usersById));
        dto.setHostNoteHistory(buildHostNoteHistory(ticket, hostNotesByTicketId, usersById));
        dto.setDeskID(ticket.getDeskID());

        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setSeverity(ticket.getSeverity());
        dto.setResolvedAt(ticket.getResolvedAt());
        dto.setEstimatedResolutionAt(ticket.getEstimatedResolutionAt());

        Long deskId = ticket.getDeskID();
        if (deskId != null) {
            String code = deskCodeByDeskId.get(deskId);
            if (code != null) {
                dto.setDeskCode(code);
            }
        }

        Long sid = ticket.getSpaceID();
        if (sid != null) {
            dto.setSpaceID(sid);
            SpaceSummary ss = spaceById.get(sid);
            if (ss != null) {
                dto.setSpaceName(ss.name());
                dto.setOfficeCode(ss.officeCode());
            }
        }

        Long workerId = ticket.getWorkerID();
        if (workerId != null) {
            dto.setWorkerID(workerId);
            User worker = usersById.get(workerId);
            if (worker != null) {
                dto.setWorkerName(worker.getName());
                dto.setWorkerSurname(worker.getSurname());
                dto.setWorkerEmail(worker.getEmail());
            }
        }

        Long techId = ticket.getTechnicianID();
        if (techId != null) {
            User tech = usersById.get(techId);
            if (tech != null) {
                dto.setAssignedTechName(tech.getName());
                dto.setAssignedTechSurname(tech.getSurname());
            }
        }

        return dto;
    }

    private List<TicketNoteMessageDTO> buildTechnicianNoteHistory(
            Ticket ticket,
            Map<Long, List<TicketTechnicianNote>> notesByTicketId,
            Map<Long, User> usersById) {
        Long ticketId = ticket.getTicketID();
        if (ticketId == null) {
            return List.of();
        }
        List<TicketTechnicianNote> stored = notesByTicketId.getOrDefault(ticketId, List.of());
        if (!stored.isEmpty()) {
            return stored.stream()
                    .map(row -> new TicketNoteMessageDTO(
                            row.getBody(),
                            row.getCreatedAt(),
                            authorChatLabel(usersById.get(row.getTechnicianID()), "Tecnico")))
                    .toList();
        }
        String legacy = ticket.getTechnicianNote();
        if (legacy == null || legacy.isBlank()) {
            return List.of();
        }
        Long techId = ticket.getTechnicianID();
        return List.of(new TicketNoteMessageDTO(
                legacy.trim(),
                ticket.getResolvedAt() != null ? ticket.getResolvedAt() : ticket.getCreatedAt(),
                authorChatLabel(techId != null ? usersById.get(techId) : null, "Tecnico")));
    }

    private List<TicketNoteMessageDTO> buildWorkerNoteHistory(
            Ticket ticket,
            Map<Long, List<TicketWorkerNote>> notesByTicketId,
            Map<Long, User> usersById) {
        Long ticketId = ticket.getTicketID();
        if (ticketId == null) {
            return List.of();
        }
        return notesByTicketId.getOrDefault(ticketId, List.of()).stream()
                .map(row -> new TicketNoteMessageDTO(
                        row.getBody(),
                        row.getCreatedAt(),
                        authorChatLabel(usersById.get(row.getWorkerID()), "Utente")))
                .toList();
    }

    private List<TicketNoteMessageDTO> buildHostNoteHistory(
            Ticket ticket,
            Map<Long, List<TicketHostNote>> notesByTicketId,
            Map<Long, User> usersById) {
        Long ticketId = ticket.getTicketID();
        if (ticketId == null) {
            return List.of();
        }
        return notesByTicketId.getOrDefault(ticketId, List.of()).stream()
                .map(row -> new TicketNoteMessageDTO(
                        row.getBody(),
                        row.getCreatedAt(),
                        authorChatLabel(usersById.get(row.getHostID()), "Host")))
                .toList();
    }

    private static String authorChatLabel(User user, String roleLabel) {
        if (user == null) {
            return roleLabel;
        }
        String fullName = fullName(user);
        return fullName.isBlank() ? roleLabel : fullName + " · " + roleLabel;
    }

    private static String fullName(User user) {
        String name = user.getName() == null ? "" : user.getName().trim();
        String surname = user.getSurname() == null ? "" : user.getSurname().trim();
        return (name + " " + surname).trim();
    }
}
