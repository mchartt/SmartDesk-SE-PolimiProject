package it.polimi.smartdesk_backend.integration;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import it.polimi.smartdesk_backend.repository.booking.BookingRepository;

/** Flusso end-to-end su server in porta casuale: admin, host, spazio, desk e prenotazione worker (date coerenti con i test). */
@FieldDefaults(level = AccessLevel.PRIVATE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BackendEndToEndFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new JdkClientHttpRequestFactory());
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
    }

    @Test
    void happyPath_allRolesThroughBookingTicketAndAudit() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> adminAuth = login("admin@test.local", "Test_seed_1!");
        String adminToken = (String) adminAuth.get("accessToken");
        Long adminId = toLong(adminAuth.get("userID"));

        Map<String, Object> hostRegisterResponse = registerHost(
                "Host " + suffix,
                "HostSur " + suffix,
                "host." + suffix + "@example.test",
                "StrongPass123!",
                "Structure " + suffix,
                "VAT" + suffix,
                """
                        Coworking e sale riunioni nel centro urbano. Servizi: fibra ottica,
                        stampa, reception e caffetteria. Orari 8–19 per freelancer e PMI.
                        Ambiente silenzioso con cabine telefono e postazioni ergonomiche.
                        """);
        Long hostId = toLong(hostRegisterResponse.get("userID"));
        String hostEmail = "host." + suffix + "@example.test";
        String technicianEmail = "tech." + suffix + "@example.test";
        String workerEmail = "worker." + suffix + "@example.test";

        ResponseEntity<List<Map<String, Object>>> pendingHostsResponse = exchange(
                "/api/admin/hosts/pending",
                HttpMethod.GET,
                null,
                authHeaders(adminToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, pendingHostsResponse.getStatusCode());
        assertTrue(pendingHostsResponse.getBody().stream()
                .anyMatch(host -> hostId.equals(toLong(host.get("userID")))));

        ResponseEntity<Void> approveHostResponse = restTemplate.exchange(
                "/api/admin/hosts/{hostID}/approve",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(adminToken)),
                Void.class,
                hostId);
        assertEquals(HttpStatus.NO_CONTENT, approveHostResponse.getStatusCode());

        Map<String, Object> hostLogin = login(hostEmail, "StrongPass123!");
        String hostToken = (String) hostLogin.get("accessToken");

        ResponseEntity<Map<String, Object>> createSpaceResponse = exchange(
                "/api/hosts",
                HttpMethod.POST,
                Map.of(
                        "name", "HQ " + suffix,
                        "address", "Milano " + suffix,
                        "city", "Milano",
                        "description", "Main office",
                        "openingHours", Map.of(
                                "MONDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "TUESDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "WEDNESDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "THURSDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "FRIDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "SATURDAY", Map.of("open", "00:00", "close", "23:59", "closed", false),
                                "SUNDAY", Map.of("open", "00:00", "close", "23:59", "closed", false)
                        )),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.CREATED, createSpaceResponse.getStatusCode());
        Long spaceId = toLong(createSpaceResponse.getBody().get("spaceID"));

        ResponseEntity<Void> approveSpaceResponse = restTemplate.exchange(
                "/api/admin/spaces/{spaceId}/approve",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(adminToken)),
                Void.class,
                spaceId);
        assertEquals(HttpStatus.NO_CONTENT, approveSpaceResponse.getStatusCode());

        ResponseEntity<List<Map<String, Object>>> hostSpacesResponse = exchange(
                "/api/hosts/{hostID}/spaces",
                HttpMethod.GET,
                null,
                authHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                },
                hostId);
        assertEquals(HttpStatus.OK, hostSpacesResponse.getStatusCode());
        assertTrue(hostSpacesResponse.getBody().stream()
                .anyMatch(space -> spaceId.equals(toLong(space.get("spaceID")))));

        ResponseEntity<Map<String, Object>> createRoomResponse = exchange(
                "/api/hosts/spaces/{spaceId}/rooms",
                HttpMethod.POST,
                Map.of(
                        "name", "Open space",
                        "code", "OS"),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                },
                spaceId);
        assertEquals(HttpStatus.CREATED, createRoomResponse.getStatusCode());
        Long roomId = toLong(createRoomResponse.getBody().get("roomID"));

        ResponseEntity<Map<String, Object>> createDeskResponse = exchange(
                "/api/hosts/desks",
                HttpMethod.POST,
                Map.of(
                        "spaceID", spaceId,
                        "roomID", roomId,
                        "amenities", List.of("wifi", "monitor", "quiet")),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.CREATED, createDeskResponse.getStatusCode());
        assertEquals("OS1", createDeskResponse.getBody().get("code"));
        Long deskId = toLong(createDeskResponse.getBody().get("id"));
        assertNotNull(deskId);

        ResponseEntity<Map<String, Object>> createTechnicianResponse = exchange(
                "/api/hosts/technicians",
                HttpMethod.POST,
                Map.of(
                        "name", "Tech " + suffix,
                        "email", technicianEmail,
                        "password", "StrongPass123!",
                        "specialization", "hardware"),
                jsonHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.CREATED, createTechnicianResponse.getStatusCode());
        Long technicianId = toLong(createTechnicianResponse.getBody().get("technicianID"));

        ResponseEntity<Map<String, Object>> assignTechnicianResponse = exchange(
                "/api/hosts/spaces/{spaceId}/technicians/{technicianId}",
                HttpMethod.POST,
                null,
                authHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                },
                spaceId,
                technicianId);
        assertEquals(HttpStatus.OK, assignTechnicianResponse.getStatusCode());
        assertEquals(technicianId.longValue(), toLong(assignTechnicianResponse.getBody().get("technicianID")));

        ResponseEntity<List<Map<String, Object>>> spaceTechniciansResponse = exchange(
                "/api/hosts/spaces/{spaceId}/technicians",
                HttpMethod.GET,
                null,
                authHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                },
                spaceId);
        assertEquals(HttpStatus.OK, spaceTechniciansResponse.getStatusCode());
        assertTrue(spaceTechniciansResponse.getBody().stream()
                .anyMatch(tech -> technicianId.equals(toLong(tech.get("technicianID")))));

        Map<String, Object> workerAuth = registerStandardUser(
                "Worker " + suffix,
                "WorkerSur " + suffix,
                workerEmail,
                "StrongPass123!",
                "WORKER",
                "Acme");
        String workerToken = (String) workerAuth.get("accessToken");
        Long workerId = toLong(workerAuth.get("userID"));

        ResponseEntity<Map<String, Object>> workerProfileResponse = exchange(
                "/api/profile",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, workerProfileResponse.getStatusCode());
        assertEquals(workerEmail, String.valueOf(workerProfileResponse.getBody().get("email")));

        ResponseEntity<List<Map<String, Object>>> spacesResponse = exchange(
                "/api/spaces",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, spacesResponse.getStatusCode());
        assertTrue(spacesResponse.getBody().stream()
                .anyMatch(space -> spaceId.equals(toLong(space.get("spaceID")))));

        ResponseEntity<Map<String, Object>> deskDetailsResponse = exchange(
                "/api/desks/{deskId}",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                },
                deskId);
        assertEquals(HttpStatus.OK, deskDetailsResponse.getStatusCode());
        assertEquals(deskId.longValue(), toLong(deskDetailsResponse.getBody().get("id")));

        LocalDateTime[] slot = bookingWindowForTest();
        LocalDateTime bookingStart = slot[0];
        LocalDateTime bookingEnd = slot[1];
        LocalDate targetDate = bookingStart.toLocalDate();
        ResponseEntity<List<Map<String, Object>>> searchResponse = exchange(
                "/api/workers/bookings/search",
                HttpMethod.POST,
                Map.of(
                        "targetDate", targetDate.toString(),
                        "city", "Milano",
                        "requiredAmenities", List.of("wifi")),
                jsonHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertFalse(searchResponse.getBody().isEmpty());

        ResponseEntity<Map<String, Object>> bookingResponse = exchange(
                "/api/workers/bookings",
                HttpMethod.POST,
                Map.of(
                        "deskID", deskId,
                        "startTime", bookingStart.toString(),
                        "end", bookingEnd.toString()),
                jsonHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        Long bookingId = toLong(bookingResponse.getBody().get("bookingID"));
        assertNotNull(bookingId);

        ResponseEntity<List<Map<String, Object>>> bookingsListResponse = exchange(
                "/api/workers/bookings",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, bookingsListResponse.getStatusCode());
        assertEquals(1, bookingsListResponse.getBody().size());
        assertEquals(deskId.longValue(), toLong(bookingsListResponse.getBody().get(0).get("deskID")));

        ResponseEntity<Map<String, Object>> bookingDetailsResponse = exchange(
                "/api/bookings/{bookingId}",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                },
                bookingId);
        assertEquals(HttpStatus.OK, bookingDetailsResponse.getStatusCode());
        assertEquals(bookingId.longValue(), toLong(bookingDetailsResponse.getBody().get("bookingID")));

        ResponseEntity<Void> subscribeResponse = exchange(
                "/api/workers/desks/{deskId}/waitlist?date={date}",
                HttpMethod.POST,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                },
                deskId,
                targetDate.toString());
        assertEquals(HttpStatus.NO_CONTENT, subscribeResponse.getStatusCode());

        if (!targetDate.equals(LocalDate.now())) {
            // WORKAROUND: ticket can only be opened for TODAY's bookings.
            // If the test ran near midnight, the booking is pushed to tomorrow,
            // so we must artificially shift it back to today in the DB to allow ticket creation.
            it.polimi.smartdesk_backend.model.booking.Booking b = bookingRepository.findById(bookingId).orElseThrow();
            b.setStartTime(LocalDateTime.now().minusMinutes(10));
            b.setEndTime(LocalDateTime.now().plusMinutes(50));
            b.setBookedDay(LocalDate.now());
            bookingRepository.save(b);
        }

        ResponseEntity<Void> reportTicketResponse = exchange(
                "/api/workers/tickets",
                HttpMethod.POST,
                Map.of(
                        "deskCode", createDeskResponse.getBody().get("code"),
                        "title", "Mouse e monitor",
                        "description", "Mouse and monitor not working",
                        "severity", "HIGH",
                        "bookingID", bookingId),
                jsonHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.CREATED, reportTicketResponse.getStatusCode());

        ResponseEntity<List<Map<String, Object>>> workerTicketsResponse = exchange(
                "/api/workers/tickets",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, workerTicketsResponse.getStatusCode());
        assertFalse(workerTicketsResponse.getBody().isEmpty());
        Long ticketId = toLong(workerTicketsResponse.getBody().get(0).get("ticketID"));

        Map<String, Object> technicianAuth = login(technicianEmail, "StrongPass123!");
        String technicianToken = (String) technicianAuth.get("accessToken");

        ResponseEntity<List<Map<String, Object>>> pendingTicketsResponse = exchange(
                "/api/technicians/tickets/pending",
                HttpMethod.GET,
                null,
                authHeaders(technicianToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, pendingTicketsResponse.getStatusCode());
        assertTrue(pendingTicketsResponse.getBody().stream()
                .anyMatch(ticket -> ticketId.equals(toLong(ticket.get("ticketID")))));

        ResponseEntity<Map<String, Object>> inProgressResponse = exchange(
                "/api/technicians/tickets/{ticketID}",
                HttpMethod.PATCH,
                Map.of("status", "IN_PROGRESS", "note", "Taking charge"),
                jsonHeaders(technicianToken),
                new ParameterizedTypeReference<>() {
                },
                ticketId);
        assertEquals(HttpStatus.OK, inProgressResponse.getStatusCode());
        assertEquals("IN_PROGRESS", String.valueOf(inProgressResponse.getBody().get("status")));

        ResponseEntity<Map<String, Object>> resolvedResponse = exchange(
                "/api/technicians/tickets/{ticketID}",
                HttpMethod.PATCH,
                Map.of(
                        "status", "RESOLVED",
                        "note", "Taking charge",
                        "resolution", "Issue fixed"),
                jsonHeaders(technicianToken),
                new ParameterizedTypeReference<>() {
                },
                ticketId);
        assertEquals(HttpStatus.OK, resolvedResponse.getStatusCode());
        assertEquals("RESOLVED", String.valueOf(resolvedResponse.getBody().get("status")));

        ResponseEntity<Void> setMaintenanceResponse = exchange(
                "/api/technicians/desks/{deskID}/maintenance",
                HttpMethod.PATCH,
                null,
                authHeaders(technicianToken),
                new ParameterizedTypeReference<>() {
                },
                deskId);
        assertEquals(HttpStatus.NO_CONTENT, setMaintenanceResponse.getStatusCode());

        HttpClientErrorException reviewBeforeCompletionError = assertThrows(
                HttpClientErrorException.class,
                () -> exchange(
                        "/api/workers/reviews",
                        HttpMethod.POST,
                        Map.of(
                                "hostID", hostId,
                                "spaceID", spaceId,
                                "bookingID", bookingId,
                                "rating", 5,
                                "comment",
                                "Esperienza complessivamente positiva, con dettagli sufficienti per la regola minima di cinquanta caratteri."),
                        jsonHeaders(workerToken),
                        new ParameterizedTypeReference<>() {
                        }));
        assertEquals(HttpStatus.BAD_REQUEST, reviewBeforeCompletionError.getStatusCode());

        ResponseEntity<List<Map<String, Object>>> hostReviewsResponse = exchange(
                "/api/hosts/{hostID}/reviews",
                HttpMethod.GET,
                null,
                authHeaders(hostToken),
                new ParameterizedTypeReference<>() {
                },
                hostId);
        assertEquals(HttpStatus.OK, hostReviewsResponse.getStatusCode());
        assertNotNull(hostReviewsResponse.getBody());

        ResponseEntity<Void> cancelBookingResponse = exchange(
                "/api/workers/bookings/{bookingId}",
                HttpMethod.DELETE,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                },
                bookingId);
        assertEquals(HttpStatus.NO_CONTENT, cancelBookingResponse.getStatusCode());

        ResponseEntity<List<Map<String, Object>>> notificationsResponse = exchange(
                "/api/profile/notifications",
                HttpMethod.GET,
                null,
                authHeaders(workerToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, notificationsResponse.getStatusCode());
        List<Map<String, Object>> notifications = notificationsResponse.getBody();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());

        ResponseEntity<List<Map<String, Object>>> logsResponse = exchange(
                "/api/admin/logs",
                HttpMethod.GET,
                null,
                authHeaders(adminToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, logsResponse.getStatusCode());
        assertFalse(logsResponse.getBody().isEmpty());

        ResponseEntity<List<Map<String, Object>>> allUsersResponse = exchange(
                "/api/admin/users",
                HttpMethod.GET,
                null,
                authHeaders(adminToken),
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, allUsersResponse.getStatusCode());
        assertTrue(allUsersResponse.getBody().stream()
                .anyMatch(user -> workerId.equals(toLong(user.get("userID")))));

        assertEquals(adminId.longValue(), toLong(adminAuth.get("userID")));
    }

    @Test
    void endpointRolesProtectedByJwt() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> workerAuth = registerStandardUser(
                "Worker " + suffix,
                "WorkerSur " + suffix,
                "worker.protected." + suffix + "@example.test",
                "StrongPass123!",
                "WORKER",
                "Acme");
        String workerToken = (String) workerAuth.get("accessToken");

        HttpClientErrorException forbiddenResponse = assertThrows(
                HttpClientErrorException.class,
                () -> exchange(
                        "/api/admin/users",
                        HttpMethod.GET,
                        null,
                        authHeaders(workerToken),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }));
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());
    }

    @Test
    void workerDeskNonExistent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> workerAuth = registerStandardUser(
                "Worker MissingDesk " + suffix,
                "WorkerSur " + suffix,
                "worker.missingdesk." + suffix + "@example.test",
                "StrongPass123!",
                "WORKER",
                "Acme");
        String workerToken = (String) workerAuth.get("accessToken");

        HttpClientErrorException notFound = assertThrows(
                HttpClientErrorException.class,
                () -> exchange(
                        "/api/desks/{deskId}",
                        HttpMethod.GET,
                        null,
                        authHeaders(workerToken),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        },
                        999_999L));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    @Test
    void bookingPayloadInvalid() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> workerAuth = registerStandardUser(
                "Worker InvalidBooking " + suffix,
                "WorkerSur " + suffix,
                "worker.invalidbooking." + suffix + "@example.test",
                "StrongPass123!",
                "WORKER",
                "Acme");
        String workerToken = (String) workerAuth.get("accessToken");

        HttpClientErrorException badRequest = assertThrows(
                HttpClientErrorException.class,
                () -> exchange(
                        "/api/workers/bookings",
                        HttpMethod.POST,
                        Map.of("end", LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString()),
                        jsonHeaders(workerToken),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }));
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());
    }

    @Test
    void quickCheck_adminEndpointWithoutToken_isRejected() {
        HttpClientErrorException unauthorized = assertThrows(
                HttpClientErrorException.class,
                () -> exchange(
                        "/api/admin/users",
                        HttpMethod.GET,
                        null,
                        authHeaders(null),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        }));
        assertEquals(HttpStatus.FORBIDDEN, unauthorized.getStatusCode());
    }

    private Map<String, Object> registerStandardUser(
            String name,
            String surname,
            String email,
            String password,
            String role,
            String company) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("surname", surname);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("role", role);
        if (company != null) {
            payload.put("company", company);
        }
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(payload, jsonHeaders(null)),
                new ParameterizedTypeReference<>() {
                });
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
        body.put("description", description.trim());
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/register/host",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(null)),
                new ParameterizedTypeReference<>() {
                });
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
                new ParameterizedTypeReference<>() {
                });
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("accessToken"));
        assertTrue(!String.valueOf(response.getBody().get("accessToken")).isBlank());
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
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            return Long.valueOf(s);
        }
        return Long.valueOf(value.toString());
    }

    private static final int BOOKING_TEST_MARGIN_MINUTES = 31;

    /** Finestra prenotazione deterministica: oggi con margine ≥30 min, altrimenti domani alle 10:00. */
    private static LocalDateTime[] bookingWindowForTest() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusMinutes(BOOKING_TEST_MARGIN_MINUTES).withSecond(0).withNano(0);

        if (!start.toLocalDate().equals(now.toLocalDate())) {
            start = now.toLocalDate().plusDays(1).atTime(10, 0);
        }

        return new LocalDateTime[] { start, start.plusMinutes(30) };
    }
}
