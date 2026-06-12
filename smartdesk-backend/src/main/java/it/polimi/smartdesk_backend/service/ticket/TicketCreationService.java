package it.polimi.smartdesk_backend.service.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.ticket.TicketDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.ticket.Ticket;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.support.codegen.CodeUtils;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servizio dedicato alla creazione e apertura dei ticket di assistenza.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCreationService {

    /** {@code kind} notifica host su nuovo ticket worker. */
    public static final String NOTIFICATION_KIND_HOST_TICKET_OPENED = "HOST_TICKET_OPENED";
    private static final String TICKET_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern DESK_CODE_LABEL_PREFIX =
            Pattern.compile("^(?i)(?:desk|postazione)(?:\\s*[-#:]\\s*|\\s+)");

    private final TicketRepository ticketRepo;
    private final DeskRepository deskRepo;
    private final SpaceRepository spaceRepo;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Apre un ticket per il worker legato alla prenotazione odierna o al codice desk (richiede booking attiva);
     * genera il codice Txxxx e notifica l'host.
     */
    @Transactional
    public Ticket openTicket(Long workerID, TicketDTO ticketDTO) {
        if (!ticketDTO.isAtLeastOneIdentifierPresent()) {
            throw new BusinessRuleException(TicketDTO.BOOKING_OR_DESK_REQUIRED);
        }
        Desk desk = ticketDTO.getBookingID() != null
                ? resolveDeskFromWorkerBooking(workerID, ticketDTO.getBookingID(), ticketDTO.getDeskCode())
                : resolveDeskForWorkerBookingToday(workerID, ticketDTO.getDeskCode());

        return saveNewTicket(workerID, desk, ticketDTO.getTitle(), ticketDTO.getDescription());
    }

    private Desk resolveDeskFromWorkerBooking(Long workerID, Long bookingID, String deskCodeInputForCheck) {
        Booking booking = bookingRepository.findById(bookingID)
                .orElseThrow(() -> new NotFoundException(TicketMessage.TICKET_BOOKING_NOT_FOUND.text()));
        if (!workerID.equals(booking.getWorkerID())) {
            throw new NotFoundException(TicketMessage.TICKET_BOOKING_NOT_FOUND.text());
        }
        LocalDate today = LocalDate.now();
        if (booking.getBookedDay() == null || !booking.getBookedDay().equals(today)) {
            throw new BusinessRuleException(TicketMessage.TICKET_ONLY_TODAY_BOOKING.text());
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessRuleException(TicketMessage.TICKET_CANCELLED_BOOKING.text());
        }
        Desk desk = deskRepo.findById(booking.getDeskID())
                .orElseThrow(() -> new NotFoundException(TicketMessage.TICKET_DESK_NOT_FOUND_GENERIC.text()));

        String normalizedInput = normalizeDeskCode(deskCodeInputForCheck);
        if (!normalizedInput.isBlank() && desk.getCode() != null && !desk.getCode().equalsIgnoreCase(normalizedInput)) {
            throw new BusinessRuleException(TicketMessage.TICKET_DESK_CODE_MISMATCH.text());
        }
        return desk;
    }

    private Desk resolveDeskForWorkerBookingToday(Long workerID, String deskCodeInput) {
        String code = normalizeDeskCode(deskCodeInput);
        if (code.isBlank()) {
            throw new BusinessRuleException(TicketDTO.BOOKING_OR_DESK_REQUIRED);
        }
        List<Desk> desks = deskRepo.findByCodeIgnoreCase(code);
        if (desks.isEmpty()) {
            throw new NotFoundException(TicketMessage.deskNotFoundByCode(deskCodeInput));
        }
        LocalDate today = LocalDate.now();
        List<Desk> eligible = desks.stream()
                .filter(d -> hasActiveBookingOn(workerID, d.getDeskID(), today))
                .toList();
        if (eligible.isEmpty()) {
            throw new BusinessRuleException(TicketMessage.TICKET_ONLY_TODAY_BOOKING.text());
        }
        if (eligible.size() > 1) {
            throw new BusinessRuleException(TicketMessage.TICKET_AMBIGUOUS_DESK_CODE.text());
        }
        return eligible.get(0);
    }

    private boolean hasActiveBookingOn(Long workerID, Long deskID, LocalDate day) {
        return bookingRepository.findByWorkerIDAndDeskID(workerID, deskID).stream()
                .anyMatch(b -> day.equals(b.getBookedDay()) && !"CANCELLED".equals(b.getStatus()));
    }

    private static String normalizeDeskCode(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        while (!t.isEmpty()) {
            Matcher m = DESK_CODE_LABEL_PREFIX.matcher(t);
            if (!m.lookingAt()) break;
            t = t.substring(m.end()).trim();
        }
        return t;
    }

    private Ticket saveNewTicket(Long workerID, Desk desk, String title, String description) {
        Ticket ticket = new Ticket();
        ticket.report(desk);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setWorkerID(workerID);
        ticket.setTitle(normalizeTitle(title));
        ticket.setDescription(description);

        Long spaceID = deskRepo.findSpaceIdByDeskID(desk.getDeskID()).orElse(null);
        ticket.setTicketCode(allocateTicketCode(spaceID));

        Ticket saved = ticketRepo.save(ticket);
        notifyHostAboutNewTicket(saved, desk, spaceID);
        return saved;
    }

    private String normalizeTitle(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) throw new BusinessRuleException(TicketDTO.TITLE_REQUIRED);
        if (title.length() > TicketDTO.TITLE_MAX_LENGTH) {
            throw new BusinessRuleException(TicketMessage.ticketTitleTooLong(TicketDTO.TITLE_MAX_LENGTH));
        }
        return title;
    }

    private String allocateTicketCode(Long spaceId) {
        Predicate<String> exists = code -> (spaceId == null)
                ? ticketRepo.existsBySpaceIDIsNullAndTicketCode(code)
                : ticketRepo.existsBySpaceIDAndTicketCode(spaceId, code);

        return CodeUtils.allocateUniqueCode(
                exists,
                64,
                TicketMessage.TICKET_CODE_ALLOCATION_FAILED.text(),
                "T",
                4,
                TICKET_CODE_ALPHABET);
    }

    private void notifyHostAboutNewTicket(Ticket ticket, Desk desk, Long spaceId) {
        if (spaceId == null) return;
        Space space = spaceRepo.findById(spaceId).orElse(null);
        if (space == null || space.getHostID() == null) return;

        String ticketRef = (ticket.getTicketCode() != null && !ticket.getTicketCode().isBlank())
                ? ticket.getTicketCode() : "#" + ticket.getTicketID();
        String deskLabel = (desk.getCode() != null && !desk.getCode().isBlank())
                ? desk.getCode() : String.valueOf(desk.getDeskID());
        String spaceLabel = (space.getName() != null && !space.getName().isBlank()) ? space.getName() : "Ufficio";

        String message = String.format("Nuova segnalazione %s sulla postazione %s — %s.", ticketRef, deskLabel, spaceLabel);
        User actor = userRepository.findById(ticket.getWorkerID()).orElse(null);
        String name = actor != null ? actor.getName() : "";
        String surname = actor != null ? actor.getSurname() : "";
        String email = actor != null ? actor.getEmail() : "";

        try {
            notificationService.sendWorkerActivityNotification(space.getHostID(), message, NOTIFICATION_KIND_HOST_TICKET_OPENED, name, surname, email, null);
        } catch (RuntimeException ex) {
            log.warn("Notifica host fallita per ticket {}: {}", ticket.getTicketID(), ex.toString());
        }
    }
}
