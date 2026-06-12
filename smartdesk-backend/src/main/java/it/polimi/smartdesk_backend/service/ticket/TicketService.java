package it.polimi.smartdesk_backend.service.ticket;

import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketSeverity;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.host.HostTechnicianSpaceManagementService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.service.ticket.state.TicketStateMachine;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Ciclo di vita ticket: gestione da host e tecnico, query e purge; la creazione è delegata a {@link TicketCreationService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    /** {@code kind} notifica host su nuovo ticket worker. */
    public static final String NOTIFICATION_KIND_HOST_TICKET_OPENED = "HOST_TICKET_OPENED";
    private static final Duration WORKER_RESOLVED_VISIBILITY = Duration.ofDays(3);

    private final TicketRepository ticketRepo;
    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final NotificationService notificationService;
    private final TicketResponseService ticketResponseService;
    private final HostOwnershipService hostOwnershipService;
    private final HostTechnicianSpaceManagementService hostTechnicianSpaceManagementService;
    private final TicketStateMachine ticketStateMachine;
    private final TicketTechnicianNoteRepository ticketTechnicianNoteRepo;
    private final TicketHostNoteRepository ticketHostNoteRepo;
    private final TicketCreationService ticketCreationService;

    // --- CREATION (delegata a TicketCreationService) ---

    /** Apre un ticket per il worker legato alla prenotazione odierna o al codice desk; delega a {@link TicketCreationService}. */
    @Transactional
    public Ticket openTicket(Long workerID, TicketDTO ticketDTO) {
        return ticketCreationService.openTicket(workerID, ticketDTO);
    }

    // --- LIFECYCLE ---

    /** Elimina il ticket: sys admin senza vincoli; il worker solo se non è IN_PROGRESS né già risolto. */
    @Transactional
    public void deleteTicketForRequester(Long ticketID, Long requesterID, Role requesterRole) {
        Ticket ticket = ticketRepo.findById(ticketID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));

        if (requesterRole == Role.SYS_ADMIN) {
            ticketRepo.delete(ticket);
            return;
        }

        if (requesterRole == Role.WORKER && requesterID.equals(ticket.getWorkerID())) {
            if (TicketStatus.fromValue(ticket.getStatus()) == TicketStatus.IN_PROGRESS) {
                throw new BusinessRuleException(TicketMessage.TICKET_DELETE_IN_PROGRESS.text());
            }
            if (!ticketStateMachine.canAddComment(ticket)) {
                throw new BusinessRuleException(TicketMessage.TICKET_DELETE_ALREADY_RESOLVED.text());
            }
            ticketRepo.delete(ticket);
            return;
        }
        throw new NotFoundException(ResourceMessage.ticketNotFound(ticketID));
    }

    /** Assegna tecnico e severità al ticket dopo verifica ownership del desk e presenza del tecnico sullo spazio. */
    @Transactional
    public TicketResponseDTO assignTechnicianToTicket(Long hostID, Long ticketID, Long technicianID, String severity) {
        Ticket ticket = ticketRepo.findById(ticketID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, ticket.getDeskID());

        Long spaceID = resolveTicketSpace(ticket);
        if (spaceID == null) {
            throw new BusinessRuleException(TicketMessage.TICKET_ASSIGN_NO_SPACE.text());
        }
        hostTechnicianSpaceManagementService.ensureTechnicianLinkedToSpace(hostID, spaceID, technicianID);

        ticketStateMachine.assignTechnician(ticket, technicianID);
        ticket.setSeverity(TicketSeverity.fromValue(severity).name());
        Ticket saved = ticketRepo.save(ticket);
        
        if (saved.getWorkerID() != null) notificationService.notifyTicketAssigned(saved.getWorkerID(), saved.getTicketCode());
        notificationService.notifyTicketAssignedToTechnician(technicianID, saved.getTicketCode());
        
        return ticketResponseService.toResponseDTO(saved);
    }

    /** Aggiorna stato, nota, severità e stima del ticket; auto-assegnazione se OPEN senza tecnico. VERIFYING/RESOLVED richiedono testo di risoluzione. */
    @Transactional
    public TicketResponseDTO updateTicketStatusForTechnician(Long technicianID, Long ticketID, String status, String note, String resolution, String severity, LocalDateTime estimatedResolutionAt) {
        Ticket ticket = ticketRepo.findById(ticketID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        
        if (ticket.getTechnicianID() != null && !ticket.getTechnicianID().equals(technicianID)) {
            throw new NotFoundException(ResourceMessage.ticketNotFound(ticketID));
        }
        if (ticket.getTechnicianID() == null) {
            ticketStateMachine.assignTechnician(ticket, technicianID);
            if (ticket.getWorkerID() != null) notificationService.notifyTicketAssigned(ticket.getWorkerID(), ticket.getTicketCode());
        }

        TicketStatus targetStatus = TicketStatus.fromValue(status);
        TicketStatus currentStatus = TicketStatus.fromValue(ticket.getStatus());

        if (severity != null && !severity.isBlank()) ticket.setSeverity(TicketSeverity.fromValue(severity).name());
        if (estimatedResolutionAt != null) ticket.setEstimatedResolutionAt(estimatedResolutionAt);

        if (targetStatus == TicketStatus.IN_PROGRESS && currentStatus == TicketStatus.OPEN) {
            if (ticket.getWorkerID() != null) notificationService.notifyTicketInProgress(ticket.getWorkerID(), ticket.getTicketCode());
        }

        boolean noteChanged = !normalizeNote(ticket.getTechnicianNote()).equals(normalizeNote(note));
        ticket.setTechnicianNote(note);

        if (targetStatus == TicketStatus.VERIFYING && currentStatus != TicketStatus.VERIFYING) {
            String res = normalizeNote(resolution);
            if (res.isEmpty()) throw new BusinessRuleException(TicketMessage.TICKET_RESOLUTION_REQUIRED.text());
            ticketStateMachine.verify(ticket, res);
            ticket.setResolvedAt(LocalDateTime.now());
            if (ticket.getWorkerID() != null) notificationService.notifyTicketVerifying(ticket.getWorkerID(), ticket.getTicketCode());
            notifyHostTicketNeedsApproval(ticket);
        }

        if (targetStatus == TicketStatus.RESOLVED && currentStatus != TicketStatus.RESOLVED) {
            String res = normalizeNote(resolution);
            if (res.isEmpty()) throw new BusinessRuleException(TicketMessage.TICKET_RESOLUTION_REQUIRED.text());
            ticketStateMachine.resolve(ticket, res);
            ticket.setResolvedAt(LocalDateTime.now());
            if (ticket.getWorkerID() != null) notificationService.notifyTicketResolved(ticket.getWorkerID(), ticket.getTicketCode());
        }

        if (noteChanged) {
            appendTechnicianNoteHistory(ticket.getTicketID(), technicianID, note);
            if (ticket.getWorkerID() != null) notificationService.notifyTicketNoteUpdated(ticket.getWorkerID(), technicianID, ticket.getTitle(), ticket.getTicketCode());
        }
        
        return ticketResponseService.toResponseDTO(ticketRepo.save(ticket));
    }

    /** Approvazione host: transizione VERIFYING → RESOLVED con notifica al worker. */
    @Transactional
    public TicketResponseDTO hostApproveTicket(Long hostID, Long ticketID) {
        Ticket ticket = ticketRepo.findById(ticketID).orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, ticket.getDeskID());
        ticketStateMachine.approve(ticket);
        Ticket saved = ticketRepo.save(ticket);
        if (saved.getWorkerID() != null) notificationService.notifyTicketResolved(saved.getWorkerID(), saved.getTicketCode());
        return ticketResponseService.toResponseDTO(saved);
    }

    /** Respinge la risoluzione: rimanda in lavorazione o riassegna a un altro tecnico; il motivo opzionale è annotato con tag [REJECT]. */
    @Transactional
    public TicketResponseDTO hostRejectTicket(Long hostID, Long ticketID, Long newTechnicianID, String reasonComment) {
        Ticket ticket = ticketRepo.findById(ticketID).orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, ticket.getDeskID());

        if (reasonComment != null && !reasonComment.isBlank()) {
            appendHostNoteHistory(ticketID, hostID, "[REJECT] " + reasonComment);
        }

        if (newTechnicianID != null && !newTechnicianID.equals(ticket.getTechnicianID())) {
            hostTechnicianSpaceManagementService.ensureTechnicianLinkedToSpace(hostID, resolveTicketSpace(ticket), newTechnicianID);
            ticketStateMachine.assignTechnician(ticket, newTechnicianID);
        } else {
            ticketStateMachine.reject(ticket);
        }

        Ticket saved = ticketRepo.save(ticket);
        if (saved.getWorkerID() != null) notificationService.notifyTicketInProgress(saved.getWorkerID(), saved.getTicketCode());
        return ticketResponseService.toResponseDTO(saved);
    }

    /** Desk dismesso: il ticket va in CLOSED con una nota host. */
    @Transactional
    public TicketResponseDTO hostDismissDesk(Long hostID, Long ticketID) {
        Ticket ticket = ticketRepo.findById(ticketID).orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        hostOwnershipService.assertDeskOwnedByHostOrNotFound(hostID, ticket.getDeskID());
        ticketStateMachine.close(ticket);
        appendHostNoteHistory(ticketID, hostID, "Desk dismesso.");
        return ticketResponseService.toResponseDTO(ticketRepo.save(ticket));
    }

    private String normalizeNote(String note) { return note == null ? "" : note.trim(); }

    private void appendTechnicianNoteHistory(Long ticketID, Long technicianID, String note) {
        String body = normalizeNote(note);
        if (!body.isEmpty() && ticketID != null) {
            ticketTechnicianNoteRepo.save(TicketTechnicianNote.of(ticketID, technicianID, body, LocalDateTime.now()));
        }
    }

    private void appendHostNoteHistory(Long ticketID, Long hostID, String note) {
        String body = normalizeNote(note);
        if (!body.isEmpty() && ticketID != null) {
            ticketHostNoteRepo.save(TicketHostNote.of(ticketID, hostID, body, LocalDateTime.now()));
        }
    }

    private void notifyHostTicketNeedsApproval(Ticket ticket) {
        Space space = spaceRepo.findById(resolveTicketSpace(ticket)).orElse(null);
        if (space != null && space.getHostID() != null) {
            notificationService.notifyHostTicketNeedsApproval(space.getHostID(), ticket.getTicketCode());
        }
    }

    private Long resolveTicketSpace(Ticket ticket) {
        if (ticket.getSpaceID() != null) return ticket.getSpaceID();
        Desk d = deskRepo.findById(ticket.getDeskID()).orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(ticket.getDeskID())));
        Long sId = d.getSpace() != null ? d.getSpace().getSpaceID() : null;
        ticket.setSpaceID(sId);
        return sId;
    }

    // --- PURGE ---

    /** Elimina lo storico ticket RESOLVED sui desk dell'host. */
    @Transactional
    public int clearResolvedTicketHistoryForHost(Long hostID) {
        return ticketRepo.deleteByStatusAndDeskHost(TicketStatus.RESOLVED.name(), hostID);
    }

    /** Elimina lo storico ticket RESOLVED assegnati al tecnico. */
    @Transactional
    public int clearResolvedTicketHistoryForTechnician(Long technicianID) {
        return ticketRepo.deleteResolvedHistoryForTechnician(technicianID);
    }

    /** Job di pulizia: ticket RESOLVED troppo vecchi (parametro retention). */
    @Transactional
    public int purgeResolvedTicketsOlderThan(Duration retention) {
        return ticketRepo.deleteByStatusAndResolvedAtBefore(
                TicketStatus.RESOLVED.name(), LocalDateTime.now().minus(retention));
    }

    // --- QUERY ---

    /** Restituisce il ticket se il richiedente è worker titolare, tecnico assegnato o sys admin; altrimenti 404. */
    @Transactional(readOnly = true)
    public Ticket getTicketByIdForRequester(Long ticketID, Long requesterID, Role requesterRole) {
        Ticket ticket = ticketRepo.findById(ticketID).orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));
        if (requesterRole == Role.SYS_ADMIN) return ticket;
        if ((requesterRole == Role.WORKER && requesterID.equals(ticket.getWorkerID())) ||
            (requesterRole == Role.TECHNICIAN && requesterID.equals(ticket.getTechnicianID()))) return ticket;
        throw new NotFoundException(ResourceMessage.ticketNotFound(ticketID));
    }

    /** Ticket ancora OPEN negli spazi dove il tecnico è abilitato. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getPendingTicketResponses(Long technicianID) {
        return ticketResponseService.toResponseDTOList(
                ticketRepo.findOpenTicketsInSpacesAssignedToTechnician(technicianID));
    }

    /** Elenca i ticket visibili al worker: attivi e risolti negli ultimi 3 giorni. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getTicketResponsesByWorker(Long workerID) {
        return ticketResponseService.toResponseDTOList(ticketRepo.findVisibleToWorker(
                workerID, LocalDateTime.now().minus(WORKER_RESOLVED_VISIBILITY),
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Storico tecnico (circa 3 mesi), dal più recente. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getTicketResponsesByTechnician(Long technicianID) {
        return ticketResponseService.toResponseDTOList(ticketRepo.findVisibleToTechnician(
                technicianID, LocalDateTime.now().minusMonths(3),
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Elenca tutti i ticket associati a un desk. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getTicketsByDesk(Long deskID) {
        return ticketResponseService.toResponseDTOList(ticketRepo.findByDeskID(deskID));
    }
    
    /** Restituisce l'archivio ticket RESOLVED dell'host; il limite è normalizzato tra 1 e 200. */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getResolvedTicketsForHost(Long hostID, int limit) {
        return ticketResponseService.toResponseDTOList(ticketRepo.findResolvedHistoryForHost(hostID).stream()
                .sorted(Comparator.comparing((Ticket t) -> t.getResolvedAt() != null ? t.getResolvedAt() : t.getCreatedAt(), Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(Math.min(Math.max(limit, 1), 200)).toList());
    }

    /** Come {@link #getTicketByIdForRequester} con mapping in {@link TicketResponseDTO}. */
    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketResponseByIdForRequester(Long ticketID, Long requesterID, Role requesterRole) {
        return ticketResponseService.toResponseDTO(getTicketByIdForRequester(ticketID, requesterID, requesterRole));
    }
}
