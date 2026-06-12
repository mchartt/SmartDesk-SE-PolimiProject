package it.polimi.smartdesk_backend.integration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import it.polimi.smartdesk_backend.repository.booking.BookingRepository;
import it.polimi.smartdesk_backend.util.message.BookingMessage;

/** Verifica i meccanismi di concorrenza sulle prenotazioni: lock pessimistico sul desk (una sola prenotazione per slot) e controllo versione / optimistic lock in riprogrammazione. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "app.seed.test-users.enabled=false",
                "security.access-token-secret=test-secret-for-integration-tests-1234567890",
                "spring.datasource.url=jdbc:h2:mem:booking_concurrency;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class BookingConcurrencyIntegrationTest {

    private static final int PARALLEL_WORKERS = 8;

    @LocalServerPort
    int port;

    @Autowired
    BookingRepository bookingRepository;

    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new JdkClientHttpRequestFactory());
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
    }

    @Test
    void concurrentBookingOnSameDeskSlot_onlyOneSucceeds() throws InterruptedException {
        DeskFixture fixture = prepareDeskWithWorkers(PARALLEL_WORKERS);
        LocalDateTime[] slot = bookingWindowForTest();
        Map<String, Object> body = Map.of(
                "deskID", fixture.deskId(),
                "startTime", slot[0].toString(),
                "end", slot[1].toString());

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_WORKERS);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(PARALLEL_WORKERS);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger deskConflict = new AtomicInteger();
        AtomicInteger otherFailure = new AtomicInteger();

        try {
            for (String token : fixture.workerTokens()) {
                pool.submit(() -> {
                    try {
                        ready.await();
                        ResponseEntity<Map<String, Object>> response = exchange(
                                "/api/workers/bookings",
                                HttpMethod.POST,
                                body,
                                jsonHeaders(token),
                                new ParameterizedTypeReference<>() {});
                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            created.incrementAndGet();
                        }
                    } catch (HttpClientErrorException ex) {
                        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST
                                && responseContains(ex, BookingMessage.DESK_ALREADY_BOOKED.text())) {
                            deskConflict.incrementAndGet();
                        } else {
                            otherFailure.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "timeout attesa thread prenotazione");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, otherFailure.get(), "errori inattesi oltre al conflitto desk");
        assertEquals(1, created.get(), "deve esistere esattamente una prenotazione confermata");
        assertEquals(PARALLEL_WORKERS - 1, deskConflict.get(), "gli altri worker devono ricevere DESK_ALREADY_BOOKED");
        assertEquals(
                1,
                bookingRepository.countDeskOverlapping(fixture.deskId(), slot[0], slot[1], null),
                "in DB deve restare un solo overlap sullo slot");
    }

    @Test
    void concurrentRescheduleWithSameVersion_onlyOneSucceeds() throws InterruptedException {
        DeskFixture fixture = prepareDeskWithWorkers(1);
        String workerToken = fixture.workerTokens().get(0);
        LocalDateTime[] slot = bookingWindowForTest();

        ResponseEntity<Map<String, Object>> bookingResponse = exchange(
                "/api/workers/bookings",
                HttpMethod.POST,
                Map.of(
                        "deskID", fixture.deskId(),
                        "startTime", slot[0].toString(),
                        "end", slot[1].toString()),
                jsonHeaders(workerToken),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        Long bookingId = toLong(bookingResponse.getBody().get("bookingID"));
        Long version = toLong(bookingResponse.getBody().get("version"));

        LocalDateTime newStartA = slot[0].plusHours(1);
        LocalDateTime newEndA = newStartA.plusMinutes(30);
        LocalDateTime newStartB = slot[0].plusHours(2);
        LocalDateTime newEndB = newStartB.plusMinutes(30);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger versionConflict = new AtomicInteger();
        AtomicInteger optimisticLock = new AtomicInteger();
        AtomicInteger otherFailure = new AtomicInteger();

        try {
            pool.submit(() -> runReschedule(
                    workerToken, bookingId, version, newStartA, newEndA,
                    ready, done, ok, versionConflict, optimisticLock, otherFailure));
            pool.submit(() -> runReschedule(
                    workerToken, bookingId, version, newStartB, newEndB,
                    ready, done, ok, versionConflict, optimisticLock, otherFailure));
            ready.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "timeout attesa thread reschedule");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, otherFailure.get(), "errori inattesi in riprogrammazione concorrente");
        assertEquals(1, ok.get(), "una sola riprogrammazione deve andare a buon fine");
        assertTrue(
                versionConflict.get() + optimisticLock.get() == 1,
                "l'altra richiesta deve fallire per versione obsoleta o OPTIMISTIC_LOCK (versionConflict="
                        + versionConflict.get()
                        + ", optimisticLock="
                        + optimisticLock.get()
                        + ")");
    }

    private void runReschedule(
            String workerToken,
            Long bookingId,
            Long version,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            CountDownLatch ready,
            CountDownLatch done,
            AtomicInteger ok,
            AtomicInteger versionConflict,
            AtomicInteger optimisticLock,
            AtomicInteger otherFailure) {
        try {
            ready.await();
            ResponseEntity<Map<String, Object>> response = exchange(
                    "/api/workers/bookings/{bookingId}",
                    HttpMethod.PATCH,
                    Map.of(
                            "bookingId", bookingId,
                            "version", version,
                            "newStart", newStart.toString(),
                            "newEnd", newEnd.toString()),
                    jsonHeaders(workerToken),
                    new ParameterizedTypeReference<>() {},
                    bookingId);
            if (response.getStatusCode() == HttpStatus.OK) {
                ok.incrementAndGet();
            }
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                if (responseContains(ex, BookingMessage.BOOKING_VERSION_STALE.text())) {
                    versionConflict.incrementAndGet();
                } else if (responseContainsCode(ex, "OPTIMISTIC_LOCK")) {
                    optimisticLock.incrementAndGet();
                } else {
                    otherFailure.incrementAndGet();
                }
            } else {
                otherFailure.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown();
        }
    }

    private DeskFixture prepareDeskWithWorkers(int workerCount) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> adminAuth = login("admin@test.local", "Test_seed_1!");
        String adminToken = (String) adminAuth.get("accessToken");

        Map<String, Object> hostRegister = registerHost(
                "Host " + suffix,
                "HostSur " + suffix,
                "host.cc." + suffix + "@example.test",
                "StrongPass123!",
                "Structure " + suffix,
                "VAT" + suffix,
                "Coworking test concurrency with enough characters in description field.");
        Long hostId = toLong(hostRegister.get("userID"));
        String hostEmail = "host.cc." + suffix + "@example.test";

        assertEquals(
                HttpStatus.NO_CONTENT,
                restTemplate.exchange(
                                "/api/admin/hosts/{hostID}/approve",
                                HttpMethod.PATCH,
                                new HttpEntity<>(authHeaders(adminToken)),
                                Void.class,
                                hostId)
                        .getStatusCode());

        String hostToken = (String) login(hostEmail, "StrongPass123!").get("accessToken");

        ResponseEntity<Map<String, Object>> spaceResponse = exchange(
                "/api/hosts",
                HttpMethod.POST,
                Map.of(
                        "name", "HQ " + suffix,
                        "address", "Milano " + suffix,
                        "city", "Milano",
                        "description", "Concurrency test space",
                        "openingHours", allDayOpeningHours()),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {});
        Long spaceId = toLong(spaceResponse.getBody().get("spaceID"));

        assertEquals(
                HttpStatus.NO_CONTENT,
                restTemplate.exchange(
                                "/api/admin/spaces/{spaceId}/approve",
                                HttpMethod.PATCH,
                                new HttpEntity<>(authHeaders(adminToken)),
                                Void.class,
                                spaceId)
                        .getStatusCode());

        ResponseEntity<Map<String, Object>> roomResponse = exchange(
                "/api/hosts/spaces/{spaceId}/rooms",
                HttpMethod.POST,
                Map.of("name", "Open", "code", "CC"),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {},
                spaceId);
        Long roomId = toLong(roomResponse.getBody().get("roomID"));

        ResponseEntity<Map<String, Object>> deskResponse = exchange(
                "/api/hosts/desks",
                HttpMethod.POST,
                Map.of("spaceID", spaceId, "roomID", roomId, "amenities", List.of("wifi")),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {});
        Long deskId = toLong(deskResponse.getBody().get("id"));

        List<String> workerTokens = new ArrayList<>(workerCount);
        for (int i = 0; i < workerCount; i++) {
            Map<String, Object> workerAuth = registerStandardUser(
                    "Worker " + suffix + i,
                    "Sur " + suffix,
                    "worker.cc." + suffix + "." + i + "@example.test",
                    "StrongPass123!",
                    "WORKER",
                    "Acme");
            workerTokens.add((String) workerAuth.get("accessToken"));
        }
        return new DeskFixture(deskId, workerTokens);
    }

    private static Map<String, Map<String, Object>> allDayOpeningHours() {
        Map<String, Object> day = Map.of("open", "00:00", "close", "23:59", "closed", false);
        return Map.of(
                "MONDAY", day,
                "TUESDAY", day,
                "WEDNESDAY", day,
                "THURSDAY", day,
                "FRIDAY", day,
                "SATURDAY", day,
                "SUNDAY", day);
    }

    private static boolean responseContains(HttpClientErrorException ex, String fragment) {
        String body = ex.getResponseBodyAsString();
        return body != null && body.contains(fragment);
    }

    private static boolean responseContainsCode(HttpClientErrorException ex, String code) {
        String body = ex.getResponseBodyAsString();
        return body != null && body.contains("\"code\":\"" + code + "\"");
    }

    private Map<String, Object> registerStandardUser(
            String name, String surname, String email, String password, String role, String company) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("surname", surname);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("role", role);
        payload.put("company", company);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(payload, jsonHeaders(null)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private Map<String, Object> registerHost(
            String name,
            String surname,
            String email,
            String password,
            String nameStructure,
            String vatNumber,
            String description) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("surname", surname);
        body.put("email", email);
        body.put("password", password);
        body.put("nameStructure", nameStructure);
        body.put("vatNumber", vatNumber);
        body.put("description", description);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/register/host",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(null)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private Map<String, Object> login(String email, String password) {
        HttpHeaders headers = jsonHeaders(null);
        headers.set("X-Forwarded-For", "127.0.0.1");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), headers),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private <T> ResponseEntity<T> exchange(
            String path,
            HttpMethod method,
            Object body,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType,
            Object... uriVariables) {
        HttpEntity<?> request = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, method, request, responseType, uriVariables);
    }

    private HttpHeaders jsonHeaders(String bearerToken) {
        HttpHeaders headers = authHeaders(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        return headers;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(value.toString());
    }

    /** Slot fisso a metà giornata: margine 30 min, stesso giorno per end, e spazio per reschedule +2h nel test di concorrenza. */
    private static LocalDateTime[] bookingWindowForTest() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minStart = now.plusMinutes(31).withSecond(0).withNano(0);

        LocalDateTime start = minStart.toLocalDate().atTime(10, 0);
        if (start.isBefore(minStart)) {
            start = minStart.toLocalDate().plusDays(1).atTime(10, 0);
        }

        LocalDateTime rescheduleHorizon = start.plusHours(2).plusMinutes(30);
        if (!start.toLocalDate().equals(rescheduleHorizon.toLocalDate())) {
            start = start.toLocalDate().plusDays(1).atTime(10, 0);
        }

        return new LocalDateTime[] {start, start.plusMinutes(30)};
    }

    private record DeskFixture(Long deskId, List<String> workerTokens) {}
}
