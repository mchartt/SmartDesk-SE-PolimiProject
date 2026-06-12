package it.polimi.smartdesk_backend.service.ticket;

import it.polimi.smartdesk_backend.dto.ticket.TicketCommentRequestDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.dto.ticket.TicketResponseDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.ticket.TicketHostNote;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.ticket.TicketTechnicianNote;
import it.polimi.smartdesk_backend.model.ticket.TicketWorkerNote;
import it.polimi.smartdesk_backend.model.user.Role;
import it.polimi.smartdesk_backend.repository.ticket.TicketHostNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketTechnicianNoteRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketWorkerNoteRepository;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.support.TextValidation;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Thread commenti sul ticket con note separate per worker, tecnico e host; i commenti sys admin sono persistiti come note tecnico con prefisso [SYSADMIN]. */
@Service
@RequiredArgsConstructor
public class TicketCommentService {

    private final TicketRepository ticketRepo;
    private final TicketWorkerNoteRepository ticketWorkerNoteRepo;
    private final TicketTechnicianNoteRepository ticketTechnicianNoteRepo;
    private final TicketHostNoteRepository ticketHostNoteRepo;
    private final TicketResponseService ticketResponseService;
    private final NotificationService notificationService;
    private final HostOwnershipService hostOwnershipService;

    /** Aggiunge il messaggio se il ruolo può vedere il ticket e non è già chiuso. Il worker riceve push se scrive qualcun altro. */
    @Transactional
    public TicketResponseDTO addComment(Long requesterID, Long ticketID, Role role, String rawBody) {
        Ticket ticket = ticketRepo.findById(ticketID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.ticketNotFound(ticketID)));

        validateAccess(ticket, requesterID, role);
        validateStatus(ticket);

        String body = TextValidation.requireTrimmed(rawBody, TicketCommentRequestDTO.BODY_REQUIRED);
        if (body.length() > TicketDTO.DESCRIPTION_MAX_LENGTH) {
            throw new BusinessRuleException(
                    TicketMessage.ticketCommentTooLong(TicketDTO.DESCRIPTION_MAX_LENGTH));
        }

        saveNote(ticketID, requesterID, role, body);
        notify(ticket, requesterID, role);

        return ticketResponseService.toResponseDTO(ticket);
    }

    private void validateAccess(Ticket ticket, Long requesterID, Role role) {
        switch (role) {
            case WORKER -> {
                if (!requesterID.equals(ticket.getWorkerID())) {
                    throw new NotFoundException(ResourceMessage.ticketNotFound(ticket.getTicketID()));
                }
            }
            case TECHNICIAN -> {
                if (!requesterID.equals(ticket.getTechnicianID())) {
                    throw new NotFoundException(ResourceMessage.ticketNotFound(ticket.getTicketID()));
                }
            }
            case HOST -> hostOwnershipService.assertDeskOwnedByHostOrNotFound(requesterID, ticket.getDeskID());
            case SYS_ADMIN -> { /* Lo sys admin può sempre commentare. */ }
            default -> throw new BusinessRuleException(TicketMessage.TICKET_COMMENT_ROLE_FORBIDDEN.text());
        }
    }

    private void validateStatus(Ticket ticket) {
        TicketStatus current = TicketStatus.fromValue(ticket.getStatus());
        if (current == TicketStatus.RESOLVED || current == TicketStatus.CLOSED) {
            throw new BusinessRuleException(TicketMessage.TICKET_COMMENT_ON_CLOSED.text());
        }
    }

    private void saveNote(Long ticketID, Long requesterID, Role role, String body) {
        LocalDateTime now = LocalDateTime.now();
        switch (role) {
            case WORKER -> ticketWorkerNoteRepo.save(TicketWorkerNote.of(ticketID, requesterID, body, now));
            case TECHNICIAN -> ticketTechnicianNoteRepo.save(TicketTechnicianNote.of(ticketID, requesterID, body, now));
            case HOST -> ticketHostNoteRepo.save(TicketHostNote.of(ticketID, requesterID, body, now));
            case SYS_ADMIN -> {
                // Per semplicità i SysAdmin commentano come tecnici o si potrebbe aggiungere una nota specifica
                ticketTechnicianNoteRepo.save(TicketTechnicianNote.of(ticketID, requesterID, "[SYSADMIN] " + body, now));
            }
        }
    }

    private void notify(Ticket ticket, Long requesterID, Role role) {
        // Solo il worker riceve avvisi su nuovi messaggi in chat se non è lui l'autore
        if (role != Role.WORKER && ticket.getWorkerID() != null) {
            notificationService.notifyTicketNoteUpdated(
                    ticket.getWorkerID(), requesterID, ticket.getTitle(), ticket.getTicketCode());
        }
    }
}
