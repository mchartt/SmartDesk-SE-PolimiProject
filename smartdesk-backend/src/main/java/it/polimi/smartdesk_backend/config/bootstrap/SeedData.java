package it.polimi.smartdesk_backend.config.bootstrap;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Struttura del file JSON di seed; senza validazioni applicative. */
@Getter
@Setter
public class SeedData {
    private List<WorkerJson> workers;
    private List<HostJson> hosts;
    private List<TechnicianJson> technicians;
    private List<SpaceJson> spaces;
    private List<BookingJson> bookings;
    private List<ReviewJson> reviews;
    private List<TicketJson> tickets;

    /** Campi comuni utente nel file seed (nome, email, password). */
    @Getter
    @Setter
    public static class UserBaseJson {
        private String name;
        private String surname;
        private String email;
        private String password;
        private boolean active = true;
    }

    /** Riga worker nel JSON demo. */
    @Getter
    @Setter
    public static class WorkerJson extends UserBaseJson {
        private String company;
    }

    /** Riga host nel JSON di seed (nome, partita IVA, flag approvazione). */
    @Getter
    @Setter
    public static class HostJson extends UserBaseJson {
        private String nameStructure;
        private String vatNumber;
        private boolean approved = false;
    }

    /** Riga tecnico nel JSON demo con host di riferimento e spazi assegnati. */
    @Getter
    @Setter
    public static class TechnicianJson extends UserBaseJson {
        private String specialisation;
        private String hostEmail;
        private List<String> assignedSpaceNames;
    }

    /** Sede coworking nel JSON demo con elenco desk annidato. */
    @Getter
    @Setter
    public static class SpaceJson {
        private String name;
        private String description;
        private String address;
        private String city;
        private String hostEmail;
        private boolean approved = false;
        private List<DeskJson> desks;
    }

    /** Postazione annidata sotto uno spazio nel JSON demo. */
    @Getter
    @Setter
    public static class DeskJson {
        private String code;
        private String building;
        private String roomCode;
        private double pricePerHour;
        private List<String> amenities;
        private String stateCode;
    }

    /** Prenotazione demo: orari assoluti o offset da oggi. */
    @Getter
    @Setter
    public static class BookingJson {
        private String workerEmail;
        private String spaceName;
        private String deskCode;
        private String startTime;
        private String endTime;
        private String status;
        private Integer daysFromNow;
        private Integer startHour;
        private Integer endHour;
    }

    /** Recensione demo legata a worker, spazio e desk. */
    @Getter
    @Setter
    public static class ReviewJson {
        private String workerEmail;
        private String spaceName;
        private String deskCode;
        private Integer daysSinceEnd;
        private int rating;
        private String comment;
        private Boolean seenByHost;
    }

    /** Ticket assistenza demo con stato e severità opzionali. */
    @Getter
    @Setter
    public static class TicketJson {
        private String workerEmail;
        private String technicianEmail;
        private String spaceName;
        private String deskCode;
        private String ticketCode;
        private String title;
        private String description;
        private String status;
        private String severity;
        private String technicianNote;
        private String resolution;
    }
}
