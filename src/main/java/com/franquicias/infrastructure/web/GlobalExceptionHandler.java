package com.franquicias.infrastructure.web;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.DatoInvalidoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Traduce excepciones a respuestas JSON uniformes (como el exception handler de DRF).
 * Así los controladores y casos de uso no tienen try/catch ni conocen códigos HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, ServerWebExchange exchange) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex, ServerWebExchange exchange) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<ErrorResponse> datoInvalido(DatoInvalidoException ex, ServerWebExchange exchange) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange, List.of());
    }

    /** Falla de @Valid en el cuerpo: lista cada campo con su problema. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> validacion(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> detalles = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos", exchange, detalles);
    }

    /** JSON mal formado, tipos incorrectos (p. ej. stock "abc"), etc. */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> entradaInvalida(ServerWebInputException ex, ServerWebExchange exchange) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", exchange, List.of());
    }

    /** Dos peticiones modificaron la misma franquicia a la vez (bloqueo optimista de @Version). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> edicionConcurrente(OptimisticLockingFailureException ex,
                                                            ServerWebExchange exchange) {
        return responder(HttpStatus.CONFLICT, "La franquicia fue modificada por otra petición, intenta de nuevo",
                exchange, List.of());
    }

    /** Violó el índice único del nombre de franquicia (carrera entre dos creaciones). */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> clavesDuplicadas(DuplicateKeyException ex, ServerWebExchange exchange) {
        return responder(HttpStatus.CONFLICT, "Ya existe una franquicia con ese nombre", exchange, List.of());
    }

    /** Errores propios de Spring, como una ruta inexistente o un método HTTP no permitido. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> estado(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatusCode status = ex.getStatusCode();
        return responder(status, ex.getReason() != null ? ex.getReason() : "Error en la petición", exchange, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, ServerWebExchange exchange) {
        log.error("Error inesperado en {}", exchange.getRequest().getPath(), ex);
        // Nunca se expone el stack trace ni el detalle interno al cliente
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", exchange, List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatusCode status, String mensaje, ServerWebExchange exchange,
                                                    List<String> detalles) {
        String error = status instanceof HttpStatus http ? http.getReasonPhrase() : status.toString();
        var cuerpo = new ErrorResponse(Instant.now(), status.value(), error, mensaje,
                exchange.getRequest().getPath().value(), detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
