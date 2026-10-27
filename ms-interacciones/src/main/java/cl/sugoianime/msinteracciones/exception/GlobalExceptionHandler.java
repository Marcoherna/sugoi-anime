package cl.sugoianime.msinteracciones.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InteraccionException.class)
    public ResponseEntity<ErrorResponse> manejarInteraccion(
            InteraccionException ex, HttpServletRequest req) {

        log.warn("{} en {}: {}", ex.getEstado(), req.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(ex.getEstado())
                .body(ErrorResponse.de(ex.getEstado().value(),
                        ex.getEstado().getReasonPhrase(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResponse(
                OffsetDateTime.now(), 400, "Bad Request",
                "Los datos enviados no son validos", req.getRequestURI(), detalles));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(
            Exception ex, HttpServletRequest req) {

        log.error("Error no controlado en {}", req.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.de(500, "Internal Server Error",
                        "Ocurrio un error inesperado", req.getRequestURI()));
    }
}
