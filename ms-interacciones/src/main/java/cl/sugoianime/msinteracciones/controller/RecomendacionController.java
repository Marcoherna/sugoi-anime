package cl.sugoianime.msinteracciones.controller;

import cl.sugoianime.msinteracciones.dto.RecomendacionDTO;
import cl.sugoianime.msinteracciones.security.PerfilGuard;
import cl.sugoianime.msinteracciones.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recomendaciones")
@Tag(name = "Recomendaciones",
        description = "Sugerencias personalizadas por filtrado basado en contenido (RF-07)")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;
    private final PerfilGuard perfilGuard;

    public RecomendacionController(RecomendacionService recomendacionService,
                                   PerfilGuard perfilGuard) {
        this.recomendacionService = recomendacionService;
        this.perfilGuard = perfilGuard;
    }

    @Operation(
            summary = "Obtener la fila 'Recomendados para ti'",
            description = """
            Implementa el requerimiento **RF-07**.

            Devuelve las recomendaciones precalculadas para el perfil, \
            ordenadas por similitud descendente.

            **Como se calculan.** Cada anime se representa como un vector \
            binario de generos. El perfil de gustos es la suma de los vectores \
            de los animes que el usuario marco como favoritos o completo. La \
            similitud de coseno entre ambos vectores da el puntaje.

            Se usa coseno en lugar de distancia euclidiana porque interesa la \
            direccion del gusto y no su magnitud: alguien que vio 3 animes y \
            alguien que vio 300 con las mismas proporciones de genero reciben \
            recomendaciones equivalentes.

            Si el perfil tiene menos de 2 interacciones, la lista viene vacia: \
            no hay senal suficiente para recomendar (problema de arranque en frio).

            **Ley 19.628.** El historial se procesa solo si el usuario otorgo \
            el consentimiento especifico para esta finalidad.
            """)
    @GetMapping
    public ResponseEntity<List<RecomendacionDTO>> obtener(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long perfilId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        return ResponseEntity.ok(recomendacionService.obtener(perfilId));
    }

    @Operation(
            summary = "Recalcular las recomendaciones del perfil",
            description = """
            Fuerza el recalculo inmediato. En operacion normal esto ocurre \
            en un job nocturno; el endpoint existe para demostraciones y \
            pruebas.
            """)
    @PostMapping("/regenerar")
    public ResponseEntity<Map<String, Object>> regenerar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long perfilId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);

        int generadas = recomendacionService.regenerar(perfilId);

        return ResponseEntity.ok(Map.of(
                "perfilId", perfilId,
                "recomendacionesGeneradas", generadas,
                "mensaje", generadas == 0
                        ? "Interacciones insuficientes para generar recomendaciones"
                        : "Recomendaciones actualizadas"));
    }
}
