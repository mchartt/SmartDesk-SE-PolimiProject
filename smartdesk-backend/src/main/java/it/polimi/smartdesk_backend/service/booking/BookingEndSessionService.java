package it.polimi.smartdesk_backend.service.booking;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.BookingDtoMapper;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.util.message.BookingMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import lombok.RequiredArgsConstructor;

/** Interruzione anticipata di una prenotazione in corso: accorcia {@code endTime} e libera gli slot residui. */
@Service
@RequiredArgsConstructor
public class BookingEndSessionService {

    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final BookingDtoMapper bookingDtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Termina la sessione attiva del worker sulla postazione, registrando l'orario di uscita.
     *
     * @param workerId proprietario della prenotazione
     * @param bookingId prenotazione da chiudere
     * @return prenotazione aggiornata con {@code endTime} = ora corrente
     */
    @Transactional
    public BookingDTO endSessionForWorker(Long workerId, Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.bookingNotFound(bookingId)));
        if (!workerId.equals(booking.getWorkerID())) {
            throw new NotFoundException(ResourceMessage.bookingNotFound(bookingId));
        }
        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            throw new BusinessRuleException(BookingMessage.BOOKING_NOT_IN_PROGRESS.text());
        }

        LocalDateTime start = booking.getStartTime();
        LocalDateTime originalEnd = booking.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (start == null || originalEnd == null || !now.isBefore(originalEnd) || start.isAfter(now)) {
            throw new BusinessRuleException(BookingMessage.BOOKING_NOT_IN_PROGRESS.text());
        }
        if (!now.isAfter(start)) {
            throw new BusinessRuleException(BookingMessage.BOOKING_LEAVE_END_AFTER_START.text());
        }

        var desk = deskRepo.lockByDeskIdForUpdate(booking.getDeskID())
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(booking.getDeskID())));

        booking.setEndTime(now);
        bookingRepo.save(booking);

        if (desk.getStateCode() != DeskStateCode.MAINTENANCE) {
            eventPublisher.publishEvent(
                    new BookingReleasedEvent(booking.getDeskID(), booking.getBookedDay(), now, originalEnd));
        }

        return bookingDtoMapper.toDto(booking, desk, null);
    }
}
