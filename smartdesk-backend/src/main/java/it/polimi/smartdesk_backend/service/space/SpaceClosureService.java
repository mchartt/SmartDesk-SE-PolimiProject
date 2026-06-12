package it.polimi.smartdesk_backend.service.space;

import it.polimi.smartdesk_backend.model.space.SpaceClosure;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.SpaceClosureCreateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceClosureDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.SpaceClosureMapper;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.service.host.HostOwnershipService;
import it.polimi.smartdesk_backend.service.space.SpaceManagementService;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.event.BookingCancelledByHostNoticeEvent;
import it.polimi.smartdesk_backend.event.BookingReleasedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Collection;
import java.util.Map;

/** Chiusure giornaliere spazio: CRUD host, lookup worker, creazione con cancellazione batch prenotazioni e eventi notifica post-commit. */
@Service
@RequiredArgsConstructor
public class SpaceClosureService {

    private final SpaceClosureRepository spaceClosureRepository;
    private final SpaceRepository spaceRepository;
    private final HostOwnershipService hostOwnershipService;
    private final SpaceManagementService spaceManagementService;
    private final SpaceClosureMapper spaceClosureMapper;
    private final BookingRepository bookingRepo;
    private final DeskRepository deskRepo;
    private final ApplicationEventPublisher eventPublisher;

    /** CRUD read: ownership verificata via {@link HostOwnershipService#loadOwnedSpaceOrNotFound}. */
    @Transactional(readOnly = true)
    public List<SpaceClosureDTO> listForHost(Long hostId, Long spaceId) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
        return spaceClosureRepository.findBySpace_SpaceIDOrderByClosedDateAsc(spaceId).stream()
                .map(spaceClosureMapper::toDto)
                .collect(Collectors.toList());
    }

    /** {@link Optional#empty()} se il giorno non è chiuso; spazio non approvato → stesso errore di spazio inesistente. */
    @Transactional(readOnly = true)
    public Optional<SpaceClosureDTO> findForWorker(Long spaceId, LocalDate date) {
        spaceManagementService.findById(spaceId); // verifica anche che lo spazio sia approvato
        return spaceClosureRepository.findBySpace_SpaceIDAndClosedDate(spaceId, date)
                .map(spaceClosureMapper::toDto);
    }

    /** Una sola transazione: prima righe {@link SpaceClosure} nuove (skip duplicate) così i nuovi giorni risultano chiusi anche per nuove prenotazioni; poi {@link #cancelActiveBookingsForSpaceOnDays} nella stessa TX cancella tutte le booking coinvolte o rollback completo; notifiche worker solo dopo COMMIT tramite Spring event listeners. */
    @Transactional
    public List<SpaceClosureDTO> createClosuresForHost(Long hostId, Long spaceId, SpaceClosureCreateRequestDTO request) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.spaceNotFound(spaceId)));

        Set<LocalDate> uniqueDays = new LinkedHashSet<>(request.getDates());
        if (uniqueDays.isEmpty()) {
            throw new BusinessRuleException(SpaceMessage.CLOSURE_DATES_NON_EMPTY.text());
        }
        LocalDate today = LocalDate.now();
        for (LocalDate d : uniqueDays) {
            if (d.isBefore(today)) {
                throw new BusinessRuleException(SpaceMessage.closurePastDay(d));
            }
        }

        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.isEmpty()) {
            throw new BusinessRuleException(SpaceMessage.CLOSURE_REASON_REQUIRED.text());
        }

        // Controllo duplicati prima del ciclo di salvataggio, per atomicità e chiarezza.
        for (LocalDate d : uniqueDays) {
            if (spaceClosureRepository.existsBySpace_SpaceIDAndClosedDate(spaceId, d)) {
                throw new BusinessRuleException(SpaceMessage.closureAlreadyExists(d));
            }
        }

        List<SpaceClosureDTO> created = new ArrayList<>();
        for (LocalDate d : uniqueDays) {
            SpaceClosure row = new SpaceClosure();
            row.setSpace(space);
            row.setClosedDate(d);
            row.setReason(reason);
            created.add(spaceClosureMapper.toDto(spaceClosureRepository.save(row)));
        }

        String spaceLabel = space.getName() != null && !space.getName().isBlank() ? space.getName().trim() : "La sede";
        cancelActiveBookingsForSpaceOnDays(spaceId, uniqueDays, spaceLabel, reason);

        return created;
    }

    /**
     * Cancella le prenotazioni attive dello spazio nei giorni indicati e pubblica gli eventi correlati.
     *
     * @param days giorni calendario da svuotare; no-op se null/vuoto
     * @param spaceLabel etichetta in notifica worker (nome spazio)
     * @param reason testo motivazione mostrato nella notifica cancellazione host
     */
    private void cancelActiveBookingsForSpaceOnDays(Long spaceId, Collection<LocalDate> days, String spaceLabel, String reason) {
        if (days == null || days.isEmpty()) {
            return;
        }
        var affected = bookingRepo.findActiveForSpaceOnBookedDays(spaceId, days).stream()
                .sorted(Comparator.comparing(Booking::getBookingID))
                .toList();
        if (affected.isEmpty()) {
            return;
        }

        // Lock dei desk coinvolti in ordine ID crescente per prevenire deadlock tra transazioni concorrenti.
        var deskIds = affected.stream()
                .map(Booking::getDeskID)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<Long, Desk> desksMap = new HashMap<>();
        for (Long deskId : deskIds) {
            var desk = deskRepo.lockByDeskIdForUpdate(deskId)
                    .orElseThrow(() -> new NotFoundException(
                            "Desk " + deskId + " non trovato durante la cancellazione massiva."));
            desksMap.put(deskId, desk);
        }

        for (Booking booking : affected) {
            Long workerId = booking.getWorkerID();
            booking.cancel();
            bookingRepo.save(booking);
            if (workerId != null) {
                eventPublisher.publishEvent(new BookingCancelledByHostNoticeEvent(
                        workerId, publicBookingRef(booking), spaceLabel, booking.getBookedDay(), reason));
            }

            var desk = desksMap.get(booking.getDeskID());
            if (desk != null && desk.getStateCode() != DeskStateCode.MAINTENANCE) {
                eventPublisher.publishEvent(
                        new BookingReleasedEvent(booking.getDeskID(), booking.getBookedDay(), booking.getStartTime(), booking.getEndTime()));
            }
        }
    }

    private static String publicBookingRef(Booking booking) {
        String code = booking.getBookingCode();
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        return String.valueOf(booking.getBookingID());
    }

    /** Rimuove la riga chiusura; non “riapre” magicamente slot già cancellati—il worker deve riprenotare. Anti-IDOR: discordanza spaceId vs closure → {@link NotFoundException}. */
    @Transactional
    public void deleteClosureForHost(Long hostId, Long spaceId, Long closureId) {
        hostOwnershipService.loadOwnedSpaceOrNotFound(hostId, spaceId);
        SpaceClosure row = spaceClosureRepository.findById(closureId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.closureNotFound(closureId)));
        if (!spaceId.equals(row.getSpace().getSpaceID())) {
            throw new NotFoundException(ResourceMessage.closureNotFound(closureId));
        }
        spaceClosureRepository.delete(row);
    }
}

