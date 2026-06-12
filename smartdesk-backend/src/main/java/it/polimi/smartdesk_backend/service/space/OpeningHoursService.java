package it.polimi.smartdesk_backend.service.space;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.smartdesk_backend.dto.space.OpeningHoursDayDTO;
import it.polimi.smartdesk_backend.dto.space.SpaceDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;
import it.polimi.smartdesk_backend.model.space.Space;
import lombok.RequiredArgsConstructor;

/** Serializza/deserializza {@code openingHours} su {@link Space}; validazione prenotazione con fascia fissa 08:00–20:00 se JSON assente. */
@Service
@RequiredArgsConstructor
public class OpeningHoursService {

    private final ObjectMapper objectMapper;
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(20, 0);

    /** {@code null}/vuoto azzera colonna JSON; payload non serializzabile → {@link BusinessRuleException}. */
    public void applyFromRequest(Space space, Map<String, OpeningHoursDayDTO> openingHours) {
        if (openingHours == null || openingHours.isEmpty()) {
            space.setOpeningHoursJson(null);
            return;
        }
        try {
            // Salvataggio diretto del JSON senza validazioni over-engineered
            space.setOpeningHoursJson(objectMapper.writeValueAsString(openingHours));
        } catch (Exception e) {
            throw new BusinessRuleException(SpaceMessage.OPENING_HOURS_INVALID_PAYLOAD.text());
        }
    }

    /** Deserializza gli orari JSON dello spazio nel DTO; payload illeggibile → {@code null}. */
    public void enrichDto(Space space, SpaceDTO dto) {
        dto.setOpeningHours(parse(space.getOpeningHoursJson()));
    }

    /**
     * Verifica che l'intero intervallo di prenotazione ricada nelle fasce di apertura dello spazio.
     *
     * @throws BusinessRuleException se la prenotazione ricade in orari di chiusura (JSON o default 08:00–20:00)
     */
    public void assertBookingWithinOpeningHours(Space space, LocalDateTime start, LocalDateTime end) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            OpeningHoursDayDTO limits = getLimitsForDay(space, date);

            if (limits.isClosed()) {
                throw new BusinessRuleException(SpaceMessage.BOOKING_OUTSIDE_OPENING_HOURS.text());
            }

            LocalTime openLimit = LocalTime.parse(limits.getOpen());
            LocalTime closeLimit = LocalTime.parse(limits.getClose());

            LocalDateTime actualStart = (date.equals(startDate)) ? start : date.atStartOfDay();
            LocalDateTime actualEnd = (date.equals(endDate)) ? end : date.atTime(LocalTime.MAX);

            if (actualStart.toLocalTime().isBefore(openLimit) || actualEnd.toLocalTime().isAfter(closeLimit)) {
                throw new BusinessRuleException(SpaceMessage.BOOKING_OUTSIDE_OPENING_HOURS.text());
            }
        }
    }

    /** Risolve gli orari per un giorno specifico: JSON se presente, altrimenti default 08:00-20:00. */
    public OpeningHoursDayDTO getLimitsForDay(Space space, LocalDate date) {
        Map<String, OpeningHoursDayDTO> hours = parseForBooking(space.getOpeningHoursJson());
        OpeningHoursDayDTO dayLimits = (hours != null) ? hours.get(date.getDayOfWeek().name()) : null;

        if (dayLimits == null || (!dayLimits.isClosed() && dayLimits.getOpen() == null && dayLimits.getClose() == null)) {
            OpeningHoursDayDTO fallback = new OpeningHoursDayDTO();
            fallback.setClosed(false);
            fallback.setOpen(DEFAULT_OPEN.toString());
            fallback.setClose(DEFAULT_CLOSE.toString());
            return fallback;
        }
        return dayLimits;
    }

    /**
     * Deserializza {@code openingHoursJson} senza propagare errori di parsing.
     *
     * @param json colonna {@code openingHoursJson}; {@code null}/blank → {@code null}
     * @return mappa giorno→fascia o {@code null} se il parsing fallisce
     */
    public Map<String, OpeningHoursDayDTO> parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, OpeningHoursDayDTO>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Come {@link #parse(String)} ma fallisce se il JSON in DB è presente ma illeggibile. */
    private Map<String, OpeningHoursDayDTO> parseForBooking(String json) {
        Map<String, OpeningHoursDayDTO> hours = parse(json);
        if (hours == null && json != null && !json.isBlank()) {
            throw new BusinessRuleException(SpaceMessage.OPENING_HOURS_INVALID_PAYLOAD.text());
        }
        return hours;
    }
}
