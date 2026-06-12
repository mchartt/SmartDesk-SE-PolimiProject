package it.polimi.smartdesk_backend.service.booking;

import it.polimi.smartdesk_backend.util.message.ResourceMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.booking.WaitlistStatusDTO;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.booking.WaitlistEntry;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.booking.WaitlistEntryRepository;
import it.polimi.smartdesk_backend.service.notification.NotificationService;
import it.polimi.smartdesk_backend.support.TimeIntervalUtils;
import lombok.RequiredArgsConstructor;

/** Lista d'attesa su desk occupati: iscrizione del worker e notifica al primo compatibile su {@link it.polimi.smartdesk_backend.event.BookingReleasedEvent}. */
@Service
@RequiredArgsConstructor
public class BookingWaitlistService {

    private final DeskRepository deskRepo;
    private final BookingRepository bookingRepo;
    private final WaitlistEntryRepository waitlistEntryRepo;
    private final NotificationService notificationService;

    /** Iscrive il worker alla lista d'attesa (o aggiorna la fascia oraria) e invia la notifica di conferma. */
    @Transactional
    public void notifyMeWhenAvailable(Long deskID, LocalDate bookedDay, Long workerID,
            LocalDateTime desiredStart, LocalDateTime desiredEnd) {
        deskRepo.findById(deskID).orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskID)));

        WaitlistEntry entry = waitlistEntryRepo.findByWorkerIDAndDeskIDAndBookedDay(workerID, deskID, bookedDay)
                .orElseGet(() -> {
                    WaitlistEntry newEntry = new WaitlistEntry();
                    newEntry.setWorkerID(workerID);
                    newEntry.setDeskID(deskID);
                    newEntry.setBookedDay(bookedDay);
                    return newEntry;
                });

        entry.setDesiredStartTime(desiredStart);
        entry.setDesiredEndTime(desiredEnd);
        entry.setNotified(false);
        waitlistEntryRepo.save(entry);

        notificationService.notifyWaitlistSubscription(workerID, deskID, bookedDay);
    }

    /** Restituisce lo stato di iscrizione del worker e se la notifica di slot libero è già stata inviata. */
    @Transactional(readOnly = true)
    public WaitlistStatusDTO getWaitlistStatus(Long deskID, LocalDate bookedDay, Long workerID) {
        Desk desk = deskRepo.findById(deskID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskID)));
        WaitlistEntry waitlistEntry =
                waitlistEntryRepo.findByWorkerIDAndDeskIDAndBookedDay(workerID, deskID, bookedDay).orElse(null);
        WaitlistStatusDTO response = new WaitlistStatusDTO();
        response.setDeskID(desk.getDeskID());
        response.setDate(bookedDay);
        response.setSubscribed(waitlistEntry != null);
        response.setNotified(waitlistEntry != null && waitlistEntry.isNotified());
        return response;
    }

    /**
     * Gestisce lo slot liberato: lock sul desk, verifica disponibilità, notifica il primo in coda compatibile.
     * {@link Propagation#REQUIRES_NEW}: l'handler {@code AFTER_COMMIT} non ha transazione attiva.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReleasedSlot(Long deskId, LocalDate day, LocalDateTime freedStart, LocalDateTime freedEnd) {
        deskRepo.lockByDeskIdForUpdate(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));

        if (bookingRepo.countDeskOverlapping(deskId, freedStart, freedEnd, null) > 0) {
            return;
        }

        List<WaitlistEntry> entries =
                waitlistEntryRepo.findByDeskIDAndBookedDayAndNotifiedFalseOrderByCreatedAtAsc(deskId, day);
        for (WaitlistEntry entry : entries) {
            boolean wantsWholeDay = entry.getDesiredStartTime() == null || entry.getDesiredEndTime() == null;
            boolean match = wantsWholeDay
                    || TimeIntervalUtils.overlaps(entry.getDesiredStartTime(), entry.getDesiredEndTime(), freedStart, freedEnd);
            if (!match) {
                continue;
            }
            notificationService.notifyDeskAvailability(entry.getWorkerID(), deskId, day);
            entry.setNotified(true);
            waitlistEntryRepo.save(entry);
            return;
        }
    }

}

