package it.polimi.smartdesk_backend.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import it.polimi.smartdesk_backend.dto.booking.BookingDTO;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.user.User;

/** Converte la prenotazione per le API, attaccandoci desk e worker se servono le etichette. */
@Mapper(componentModel = "spring")
public interface BookingDtoMapper {

    @Mapping(target = "deskID", source = "booking.deskID")
    @Mapping(target = "version", source = "booking.version")
    @Mapping(target = "workerID", source = "booking.workerID")
    @Mapping(target = "bookedDay", source = "booking.bookedDay")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "deskCode", source = "desk.code")
    @Mapping(target = "buildingName", source = "desk.building")
    @Mapping(target = "spaceName", source = "desk.space.name")
    @Mapping(target = "city", source = "desk.space.city")
    @Mapping(target = "workerEmail", source = "worker.email")
    @Mapping(target = "workerName", source = "worker", qualifiedByName = "toFullName")
    /** Mette insieme prenotazione, desk e utente per farli vedere nelle liste del frontend. */
    BookingDTO toDto(Booking booking, Desk desk, User worker);

    /** Crea l'oggetto da mandare con le info base della prenotazione. */
    static BookingDTO fromBookingSkeleton(Booking booking) {
        BookingDTO dto = new BookingDTO(
                booking.getBookingID(),
                booking.getDeskID(),
                booking.getStartTime(),
                booking.getEndTime());
        dto.setBookedDay(booking.getBookedDay());
        dto.setVersion(booking.getVersion());
        return dto;
    }

    /** Converte tante prenotazioni insieme pescando dalle mappe per non fare query inutili. */
    default List<BookingDTO> toDtoList(List<Booking> bookings, Map<Long, Desk> desksById, Map<Long, User> usersById) {
        if (bookings == null || bookings.isEmpty()) return List.of();
        return bookings.stream()
                .map(b -> toDto(b, desksById.get(b.getDeskID()), usersById.get(b.getWorkerID())))
                .collect(Collectors.toList());
    }

    /** Nome e cognome trimmati; {@code null} se mancano entrambi. */
    @Named("toFullName")
    default String toFullName(User user) {
        if (user == null) return null;
        String given = user.getName() != null ? user.getName().trim() : "";
        String family = user.getSurname() != null ? user.getSurname().trim() : "";
        String fullName = (given + " " + family).trim();
        return fullName.isEmpty() ? null : fullName;
    }
}
