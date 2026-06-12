package it.polimi.smartdesk_backend.support;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Corpo errore JSON uniforme ({@code errorId}, {@code code}, {@code fieldErrors}) per filter e {@link it.polimi.smartdesk_backend.exception.RestExceptionHandler}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiErrorSupport {

    /**
     * @param fieldErrors {@code null} → lista vuota nel JSON
     * @return mappa pronta per serializzazione; {@code path} = {@code N/A} se request assente
     */
    public static Map<String, Object> createErrorBody(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            List<Map<String, String>> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorId", UUID.randomUUID().toString());
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", request == null ? "N/A" : request.getRequestURI());
        body.put("fieldErrors", fieldErrors == null ? List.of() : fieldErrors);
        return body;
    }

    /** Scrive JSON errore su response già con status HTTP impostato. */
    public static void writeErrorResponse(
            ObjectMapper objectMapper,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {
        Map<String, Object> body = createErrorBody(request, status, code, message, List.of());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.writeValue(response.getWriter(), body);
    }
}

