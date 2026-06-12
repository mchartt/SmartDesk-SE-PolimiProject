package it.polimi.smartdesk_backend.exception;

import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.HttpMessage;
import it.polimi.smartdesk_backend.support.ApiErrorSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

/** Mapping centralizzato eccezioni Spring/dominio → JSON errore ({@link it.polimi.smartdesk_backend.support.ApiErrorSupport}). */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /** Risorsa assente o mascherata come 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                exception.getMessage(),
                List.of(),
                request,
                false,
                exception);
    }

    /** Regola di dominio violata (slot, orari, stato account, …). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BUSINESS_RULE_VIOLATION",
                exception.getMessage(),
                List.of(),
                request,
                false,
                exception);
    }

    /** Utente autenticato ma senza permesso sull'operazione. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), List.of(), request, false, exception);
    }

    /** Token assente, scaduto o non valido. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage(), List.of(), request, false, exception);
    }

    /** Spring Security 6+ ha negato l'accesso (@PreAuthorize, ecc.). */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(
            AuthorizationDeniedException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", AuthMessage.ACCESS_DENIED.text(), List.of(), request, false, exception);
    }

    /** Errori {@code @Valid} su {@code @RequestBody}: elenco campi in {@code fieldErrors}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fieldError.getField());
            item.put("message", fieldError.getDefaultMessage());
            fieldErrors.add(item);
        }

        exception.getBindingResult().getGlobalErrors().forEach(globalError -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", globalError.getObjectName());
            item.put("message", globalError.getDefaultMessage());
            fieldErrors.add(item);
        });

        String message = validationSummaryFrom(fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, fieldErrors, request, false, exception);
    }

    /** Concatena i messaggi campo per il summary principale. */
    private static String validationSummaryFrom(List<Map<String, String>> fieldErrors) {
        if (fieldErrors.isEmpty()) {
            return "Richiesta non valida.";
        }
        String joined = fieldErrors.stream()
                .map(e -> e.get("message"))
                .filter(m -> m != null && !m.isBlank())
                .collect(Collectors.joining(" "));
        return joined.isBlank() ? "Richiesta non valida." : joined;
    }

    /** Validazione su parametri metodo controller (Spring 6.1+). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = List.of(errorDetail("parameter", "method-parameters", exception.getMessage()));
        String message = validationSummaryFrom(fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, fieldErrors, request, false, exception);
    }

    /** Violazioni Jakarta Validation fuori dal binding MVC (es. parametri singoli). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        exception.getConstraintViolations().forEach(violation -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", violation.getPropertyPath().toString());
            item.put("message", violation.getMessage());
            fieldErrors.add(item);
        });
        String message = validationSummaryFrom(fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, fieldErrors, request, false, exception);
    }

    /** JSON malformato o tipo non deserializzabile. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                HttpMessage.MALFORMED_JSON_BODY.text(),
                List.of(),
                request,
                false,
                exception);
    }

    /** Header obbligatorio assente (es. Authorization su endpoint protetti). */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request) {
        String message = HttpMessage.missingHeader(exception.getHeaderName());
        List<Map<String, String>> details = List.of(errorDetail("header", exception.getHeaderName(), message));
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_HEADER", message, details, request, false, exception);
    }

    /** Query o form parameter richiesto ma non inviato. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        String message = HttpMessage.missingParameter(exception.getParameterName());
        List<Map<String, String>> details = List.of(errorDetail("parameter", exception.getParameterName(), message));
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, details, request, false, exception);
    }

    /** Parametro presente ma non convertibile (es. {@code abc} dove serve {@code Long}). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String message = HttpMessage.invalidQueryParameter(exception.getName());
        List<Map<String, String>> details = List.of(errorDetail(
                "parameter",
                exception.getName(),
                HttpMessage.typeMismatchExpectedType(
                        exception.getRequiredType() == null ? null : exception.getRequiredType().getSimpleName())));
        return buildResponse(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, details, request, false, exception);
    }

    /** Verb HTTP non ammesso sulla route (GET vs POST, …). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        String message = HttpMessage.httpMethodNotSupported(exception.getMethod());
        List<String> supported = exception.getSupportedHttpMethods() == null
                ? Collections.emptyList()
                // Usa HttpMethod::name se sei su Spring 6+, oppure String::valueOf per sicurezza
                : exception.getSupportedHttpMethods().stream().map(method -> method.name()).toList();
        List<Map<String, String>> details = supported.isEmpty()
                ? List.of()
                : List.of(errorDetail("supportedMethods", String.join(",", supported), HttpMessage.SUPPORTED_METHODS_LABEL.text()));
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", message, details, request, false, exception);
    }

    /** Collisione esplicita di dominio (versione, univocità, …). */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), List.of(), request, false, exception);
    }

    /** Conflitto di lock ottimistico: il client deve ricaricare la risorsa. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "OPTIMISTIC_LOCK",
                "I dati sono stati modificati nel frattempo. Ricarica e riprova.",
                List.of(),
                request,
                false,
                exception);
    }

    /** Vincolo DB (unique, FK): messaggio generico, dettaglio SQL solo nei log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                HttpMessage.DATA_INTEGRITY_USER_MESSAGE.text(),
                List.of(),
                request,
                true,
                exception);
    }

    /** Rete di sicurezza: qualsiasi errore non mappato sopra → 500 con messaggio sobrio. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                HttpMessage.INTERNAL_ERROR_USER.text(),
                List.of(),
                request,
                true,
                exception);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String code,
            String message,
            List<Map<String, String>> fieldErrors,
            HttpServletRequest request,
            boolean serverErrorLog,
            Exception exception) {
        Map<String, Object> body = ApiErrorSupport.createErrorBody(request, status, code, message, fieldErrors);
        String errorId = (String) body.get("errorId");
        String path = (String) body.get("path");
        if (serverErrorLog) {
            log.error("ErrorId={} status={} code={} path={} message={}", errorId, status.value(), code, path, message, exception);
        } else {
            log.warn("ErrorId={} status={} code={} path={} message={}", errorId, status.value(), code, path, message);
        }
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, String> errorDetail(String kind, String name, String message) {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("kind", kind);
        detail.put("name", name);
        detail.put("message", message);
        return detail;
    }
}

