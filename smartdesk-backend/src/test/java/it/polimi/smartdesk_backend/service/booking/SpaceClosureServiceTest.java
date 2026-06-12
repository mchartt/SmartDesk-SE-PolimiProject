package it.polimi.smartdesk_backend.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.polimi.smartdesk_backend.service.space.SpaceClosureService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import it.polimi.smartdesk_backend.dto.space.SpaceClosureCreateRequestDTO;
import it.polimi.smartdesk_backend.model.booking.Booking;
import it.polimi.smartdesk_backend.model.booking.BookingStatus;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceClosureRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
class SpaceClosureServiceTest {

    @Autowired
    private SpaceClosureService spaceClosureService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceClosureRepository spaceClosureRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    private Long hostId;
    private Long spaceId;
    private Long bookingId;
    private LocalDate closureDate;

    @BeforeEach
    void setUp() {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Host host = new Host();
        host.setName("Host");
        host.setSurname("Test");
        host.setEmail("host.closure." + suffix + "@test.it");
        host.setPassword("Pass123!");
        host.setApproved(true);
        host = hostRepository.save(host);
        hostId = host.getId();

        Space space = new Space();
        space.setName("Space Test");
        space.setHostID(hostId);
        space.setApproved(true);
        space = spaceRepository.save(space);
        spaceId = space.getSpaceID();

        Desk desk = new Desk();
        desk.setCode("D1");
        desk.setSpace(space);
        entityManager.persist(desk);

        closureDate = LocalDate.now().plusDays(5);
        Booking booking = new Booking();
        booking.setWorkerID(999L);
        booking.setDeskID(desk.getDeskID());
        booking.setBookedDay(closureDate);
        booking.setStartTime(closureDate.atTime(10, 0));
        booking.setEndTime(closureDate.atTime(12, 0));
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking = bookingRepository.save(booking);
        bookingId = booking.getBookingID();

        transactionManager.commit(status);
    }

    @Test
    @Transactional
    void createClosuresRollbackUndoesCancellations() {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED.name(), booking.getStatus());

        SpaceClosureCreateRequestDTO request = new SpaceClosureCreateRequestDTO();
        request.setDates(List.of(closureDate));
        request.setReason("Lavori in corso");

        spaceClosureService.createClosuresForHost(hostId, spaceId, request);

        entityManager.flush();
        entityManager.clear();
        
        Booking updated = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CANCELLED.name(), updated.getStatus());
    }

    @Test
    @Transactional
    public void verifiesClosureCreationAndBookingStatus() {
        SpaceClosureCreateRequestDTO request = new SpaceClosureCreateRequestDTO();
        request.setDates(List.of(closureDate));
        request.setReason("Test Semplificato");

        spaceClosureService.createClosuresForHost(hostId, spaceId, request);
        
        entityManager.flush();
        entityManager.clear();

        // Verifichiamo che la chiusura esista
        boolean closureExists = spaceClosureRepository.findBySpace_SpaceIDAndClosedDate(spaceId, closureDate).isPresent();
        assertEquals(true, closureExists, "La chiusura deve essere stata salvata");

        // Verifichiamo che la prenotazione sia stata cancellata
        Booking updated = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CANCELLED.name(), updated.getStatus(), "La prenotazione deve essere CANCELLED");
    }
}

