package it.polimi.smartdesk_backend.service.booking;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.dto.booking.BookingRequestDTO;
import it.polimi.smartdesk_backend.dto.booking.RescheduleBookingDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ConflictException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.support.codegen.CodeUtils;
import it.polimi.smartdesk_backend.util.message.BookingMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.policy.BookingTimeRules;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Crea nuove prenotazioni e le sposta nel tempo (reschedule). Incrocia regole temporali, overlap worker/desk, stato del desk e orari di apertura dello spazio. */
@Service
@RequiredArgsConstructor
public class BookingCreationService {

    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final BookingDtoMapper bookingDtoMapper;
    private final BookingTimeRules bookingTimeRules;
    private final DeskStateMachine deskStateMachine;
    private final ApplicationEventPublisher eventPublisher;

    /** Orari apertura spazio: {@code assertBookingWithinOpeningHours} su questo service. */
    private final SpaceManagementService spaceManagementService;

    /**
     * Conferma una prenotazione per il worker sul desk richiesto.
     * Il lock pessimistico sul desk serializza verifica e insert per evitare doppia prenotazione concorrente sullo stesso intervallo.
     *
     * @param workerID worker che prenota
     * @param request desk, intervallo start/end (stesso giorno, nel futuro, finestra ammessa)
     * @return prenotazione salvata con dettagli desk
     * @throws BusinessRuleException vincoli temporali, overlap, desk non prenotabile, spazio chiuso
     * @throws NotFoundException desk inesistente
     */
    @Transactional
    public BookingDTO createBooking(Long workerID, BookingRequestDTO request) {
        LocalDateTime endTime = request.getEnd();
        LocalDateTime startTime = request.getStartTime();
        if (startTime == null) {
            throw new BusinessRuleException(BookingMessage.START_TIME_REQUIRED.text());
        }
        if (endTime == null) {
            throw new BusinessRuleException(BookingMessage.END_TIME_REQUIRED.text());
        }
        LocalDate bookingDay = startTime.toLocalDate();
        LocalDateTime now = LocalDateTime.now();

        assertValidInterval(startTime, endTime, BookingMessage.END_AFTER_START);
        assertStartTimeBookable(startTime, now, BookingMessage.START_IN_FUTURE);

        assertBookingDayInWindow(bookingDay);

        Desk desk = deskRepo.lockByDeskIdForUpdate(request.getDeskID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(request.getDeskID())));

        if (bookingRepo.countWorkerOverlapping(workerID, startTime, endTime, null) > 0) {
            throw new BusinessRuleException(BookingMessage.WORKER_SLOT_OVERLAP.text());
        }

        deskStateMachine.assertBookable(desk);
        spaceManagementService.assertSpaceOpenOnCalendarDay(desk.getSpace(), bookingDay);
        spaceManagementService.assertBookingWithinOpeningHours(desk.getSpace(), startTime, endTime);
        long deskOverlap = bookingRepo.countDeskOverlapping(request.getDeskID(), startTime, endTime, null);
        if (deskOverlap > 0) {
            throw new BusinessRuleException(BookingMessage.DESK_ALREADY_BOOKED.text());
        }

        Booking booking = new Booking();
        booking.setWorkerID(workerID);
        booking.setDeskID(request.getDeskID());
        booking.setBookedDay(bookingDay);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setBookingCode(CodeUtils.allocateUniqueCode(bookingRepo::existsByBookingCode, 64, "BOOKING_CODE"));

        try {
            return bookingDtoMapper.toDto(bookingRepo.save(booking), desk, null);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(BookingMessage.BOOKING_CODE_CONFLICT.text());
        }
    }

    /**
     * Ripianifica l'orario di una prenotazione CONFIRMED o PENDING del worker.
     * Richiede ownership, versione ottimistica coerente e ri-validazione completa dello slot;
     * emette {@link BookingReleasedEvent} se lo slot precedente si libera.
     */
    @Transactional
    public BookingDTO rescheduleBooking(Long workerId, Long bookingId, RescheduleBookingDTO dto) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingId)));
        if (!workerId.equals(booking.getWorkerID())) {
            throw new NotFoundException(ResourceMessage.bookingNotFound(bookingId));
        }
        BookingStatus currentStatus;
        try {
            currentStatus = BookingStatus.valueOf(booking.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(BookingMessage.BOOKING_STATUS_UNKNOWN.text());
        }
        if (currentStatus != BookingStatus.PENDING && currentStatus != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException(BookingMessage.BOOKING_CANNOT_RESCHEDULE.text());
        }
        Long expectedVersion = dto.getVersion();
        Long currentVersion = booking.getVersion();
        long currentVersionValue = currentVersion == null ? 0L : currentVersion;
        if (expectedVersion == null || !expectedVersion.equals(currentVersionValue)) {
            throw new ConflictException(BookingMessage.BOOKING_VERSION_STALE.text());
        }

        LocalDateTime newStart = dto.getNewStart();
        LocalDateTime newEnd = dto.getNewEnd();
        
        assertValidInterval(newStart, newEnd, BookingMessage.RESCHEDULE_END_AFTER_START);
        LocalDateTime now = LocalDateTime.now();
        assertStartTimeBookable(newStart, now, BookingMessage.RESCHEDULE_START_FUTURE);

        LocalDate newBookingDay = newStart.toLocalDate();
        assertBookingDayInWindow(newBookingDay);

        LocalDateTime previousStart = booking.getStartTime();
        LocalDateTime previousEnd = booking.getEndTime();
        LocalDate previousDay = booking.getBookedDay();

        Desk desk = deskRepo.lockByDeskIdForUpdate(booking.getDeskID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(booking.getDeskID())));

        deskStateMachine.assertBookable(desk);

        if (bookingRepo.countWorkerOverlapping(workerId, newStart, newEnd, bookingId) > 0) {
            throw new BusinessRuleException(BookingMessage.WORKER_SLOT_OVERLAP.text());
        }

        spaceManagementService.assertSpaceOpenOnCalendarDay(desk.getSpace(), newBookingDay);
        spaceManagementService.assertBookingWithinOpeningHours(desk.getSpace(), newStart, newEnd);
        if (bookingRepo.countDeskOverlapping(booking.getDeskID(), newStart, newEnd, bookingId) > 0) {
            throw new BusinessRuleException(BookingMessage.DESK_ALREADY_BOOKED.text());
        }
        booking.setStartTime(newStart);
        booking.setEndTime(newEnd);
        booking.setBookedDay(newBookingDay);
        BookingDTO saved = bookingDtoMapper.toDto(bookingRepo.save(booking), desk, null);

        if (previousStart != null && previousEnd != null && previousDay != null
                && desk.getStateCode() != DeskStateCode.MAINTENANCE
                && slotChanged(previousStart, previousEnd, previousDay, newStart, newEnd, newBookingDay)) {
            eventPublisher.publishEvent(
                    new BookingReleasedEvent(booking.getDeskID(), previousDay, previousStart, previousEnd));
        }
        return saved;
    }

    private void assertBookingDayInWindow(LocalDate bookingDay) {
        LocalDate today = LocalDate.now();
        if (!bookingTimeRules.isBookingDayAllowed(bookingDay, today)) {
            throw new BusinessRuleException(BookingMessage.BOOKING_DAY_WINDOW.text());
        }
    }

    private void assertValidInterval(LocalDateTime start, LocalDateTime end, BookingMessage orderError) {
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new BusinessRuleException(orderError.text());
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessRuleException(BookingMessage.START_END_SAME_DAY.text());
        }
    }

    /** Passato → messaggio specifico; meno di 30 minuti di margine → messaggio dedicato. */
    private void assertStartTimeBookable(LocalDateTime startTime, LocalDateTime now, BookingMessage futureError) {
        if (startTime.isBefore(now)) {
            throw new BusinessRuleException(futureError.text());
        }
        if (!bookingTimeRules.firstBookingSlotStillOpen(startTime, now)) {
            throw new BusinessRuleException(BookingMessage.START_TIME_TOO_CLOSE.text());
        }
    }

    private static boolean slotChanged(
            LocalDateTime previousStart, LocalDateTime previousEnd, LocalDate previousDay,
            LocalDateTime newStart, LocalDateTime newEnd, LocalDate newDay) {
        return !previousStart.equals(newStart) || !previousEnd.equals(newEnd) || !previousDay.equals(newDay);
    }
}

