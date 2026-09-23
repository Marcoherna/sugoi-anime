package cl.sugoianime.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Respuestas de respaldo cuando un microservicio no responde.
 *
 * Sin estos fallbacks, un microservicio caido produce un error opaco de
 * Netty. Con ellos, el cliente recibe un 503 explicito que indica QUE
 * servicio fallo y que el resto del sistema sigue operativo.
 *
 * Es la diferencia entre "la aplicacion se cayo" y "la seccion de
 * recomendaciones no esta disponible por ahora".
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/catalogo")
    public Mono<ResponseEntity<Map<String, Object>>> catalogo() {
        return respuesta("Catalogo",
                "El catalogo no esta disponible temporalmente. Intenta en unos momentos.");
    }

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> auth() {
        return respuesta("Identidad",
                "El servicio de autenticacion no esta disponible temporalmente.");
    }

    @RequestMapping("/interacciones")
    public Mono<ResponseEntity<Map<String, Object>>> interacciones() {
        return respuesta("Interacciones",
                "Tus favoritos y recomendaciones no estan disponibles temporalmente.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> respuesta(
            String servicio, String mensaje) {

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "momento", OffsetDateTime.now().toString(),
                        "estado", 503,
                        "error", "Service Unavailable",
                        "servicio", servicio,
                        "mensaje", mensaje,
                        "circuitoAbierto", true)));
    }
}
