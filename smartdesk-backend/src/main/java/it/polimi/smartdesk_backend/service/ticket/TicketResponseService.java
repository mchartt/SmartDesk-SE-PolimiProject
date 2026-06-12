package it.polimi.smartdesk_backend.service.ticket;

import it.polimi.smartdesk_backend.dto.common.SpaceSummary;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.mapper.TicketResponseMapper;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Composizione di {@link TicketResponseDTO} con dati correlati recuperati dai repository. */
@Service
@RequiredArgsConstructor
public class TicketResponseService {

    private final TicketResponseMapper ticketResponseMapper;
    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepository;
    private final TicketTechnicianNoteRepository ticketTechnicianNoteRepo;
    private final TicketWorkerNoteRepository ticketWorkerNoteRepo;
    private final TicketHostNoteRepository ticketHostNoteRepo;

    /** Arricchisce un ticket con spazio, note e dati degli attori coinvolti. */
    @Transactional(readOnly = true)
    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        return toResponseDTOList(List.of(ticket)).get(0);
    }

    /** Mappa una lista di ticket con prefetch batch per evitare query N+1. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> toResponseDTOList(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }

        Set<Long> deskIds = new HashSet<>();
        Set<Long> spaceIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket.getDeskID() != null) deskIds.add(ticket.getDeskID());
            if (ticket.getSpaceID() != null) spaceIds.add(ticket.getSpaceID());
            if (ticket.getWorkerID() != null) userIds.add(ticket.getWorkerID());
            if (ticket.getTechnicianID() != null) userIds.add(ticket.getTechnicianID());
        }

        Set<Long> ticketIds = tickets.stream()
                .map(Ticket::getTicketID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<TicketTechnicianNote>> techNotes = loadTechnicianNotes(ticketIds);
        Map<Long, List<TicketWorkerNote>> workerNotes = loadWorkerNotes(ticketIds);
        Map<Long, List<TicketHostNote>> hostNotes = loadHostNotes(ticketIds);

        collectNoteAuthorUserIds(userIds, techNotes, workerNotes, hostNotes);

        Map<Long, String> deskCodes = loadDeskCodes(deskIds);
        Map<Long, SpaceSummary> spaceSummaries = loadSpaceSummaries(spaceIds);
        Map<Long, User> users = userIds.isEmpty() ? Map.of() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        return tickets.stream()
                .map(ticket -> ticketResponseMapper.buildResponseDTO(
                        ticket, deskCodes, spaceSummaries, users, techNotes, workerNotes, hostNotes))
                .toList();
    }

    private void collectNoteAuthorUserIds(
            Set<Long> userIds,
            Map<Long, List<TicketTechnicianNote>> techNotes,
            Map<Long, List<TicketWorkerNote>> workerNotes,
            Map<Long, List<TicketHostNote>> hostNotes) {
        techNotes.values().forEach(rows -> rows.forEach(row -> { if (row.getTechnicianID() != null) userIds.add(row.getTechnicianID()); }));
        workerNotes.values().forEach(rows -> rows.forEach(row -> { if (row.getWorkerID() != null) userIds.add(row.getWorkerID()); }));
        hostNotes.values().forEach(rows -> rows.forEach(row -> { if (row.getHostID() != null) userIds.add(row.getHostID()); }));
    }

    private Map<Long, List<TicketTechnicianNote>> loadTechnicianNotes(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) return Map.of();
        Map<Long, List<TicketTechnicianNote>> grouped = new HashMap<>();
        ticketTechnicianNoteRepo.findByTicketIDInOrderByCreatedAtAsc(ticketIds)
                .forEach(row -> grouped.computeIfAbsent(row.getTicketID(), k -> new ArrayList<>()).add(row));
        return grouped;
    }

    private Map<Long, List<TicketWorkerNote>> loadWorkerNotes(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) return Map.of();
        Map<Long, List<TicketWorkerNote>> grouped = new HashMap<>();
        ticketWorkerNoteRepo.findByTicketIDInOrderByCreatedAtAsc(ticketIds)
                .forEach(row -> grouped.computeIfAbsent(row.getTicketID(), k -> new ArrayList<>()).add(row));
        return grouped;
    }

    private Map<Long, List<TicketHostNote>> loadHostNotes(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) return Map.of();
        Map<Long, List<TicketHostNote>> grouped = new HashMap<>();
        ticketHostNoteRepo.findByTicketIDInOrderByCreatedAtAsc(ticketIds)
                .forEach(row -> grouped.computeIfAbsent(row.getTicketID(), k -> new ArrayList<>()).add(row));
        return grouped;
    }

    private Map<Long, String> loadDeskCodes(Set<Long> deskIds) {
        if (deskIds.isEmpty()) return Map.of();
        Map<Long, String> byDeskId = new HashMap<>();
        deskRepo.findAllById(deskIds).forEach(desk -> byDeskId.put(desk.getDeskID(), desk.getCode()));
        return byDeskId;
    }

    private Map<Long, SpaceSummary> loadSpaceSummaries(Set<Long> spaceIds) {
        if (spaceIds.isEmpty()) return Map.of();
        Map<Long, SpaceSummary> bySpaceId = new HashMap<>();
        spaceRepo.findAllById(spaceIds).forEach(space -> bySpaceId.put(
                space.getSpaceID(),
                new SpaceSummary(space.getName(), space.getOfficeCode())));
        return bySpaceId;
    }
}
