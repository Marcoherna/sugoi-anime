package cl.sugoianime.msinteracciones.controller;

import cl.sugoianime.msinteracciones.dto.ContinuarViendoDTO;
import cl.sugoianime.msinteracciones.dto.GuardarProgresoRequest;
import cl.sugoianime.msinteracciones.dto.ProgresoDTO;
import cl.sugoianime.msinteracciones.security.PerfilGuard;
import cl.sugoianime.msinteracciones.service.ProgresoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/progreso")
@Tag(name = "Progreso",
        description = "Punto de reproduccion por perfil y episodio (RF-03)")
public class ProgresoController {

    private final ProgresoService progresoService;
    private final PerfilGuard perfilGuard;

    public ProgresoController(ProgresoService progresoService, PerfilGuard perfilGuard) {
        this.progresoService = progresoService;
        this.perfilGuard = perfilGuard;
    }

    @Operation(
            summary = "Guardar el punto de reproduccion",
            description = """
            Implementa el requerimiento **RF-03**.

            Operacion idempotente: existe un unico registro por perfil y \
            episodio, que se sobreescribe.

            El frontend debe llamar a este endpoint con throttling (cada 10 \
            segundos, mas al pausar y al cerrar), nunca en cada segundo de \
            reproduccion.

            El episodio se marca como completado automaticamente al superar \
            el 90% de la duracion.
            """)
    @PutMapping
    public ResponseEntity<ProgresoDTO> guardar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GuardarProgresoRequest request) {

        perfilGuard.verificarPropiedad(jwt, request.perfilId());

        return ResponseEntity.ok(
                progresoService.guardar(perfilGuard.usuarioIdDe(jwt), request));
    }

    @Operation(
            summary = "Obtener el progreso de un episodio",
            description = """
            Si no hay progreso previo devuelve segundo 0, no un 404: empezar \
            un episodio desde el principio es un caso normal, no un error.
            """)
    @GetMapping("/episodio/{episodioId}")
    public ResponseEntity<ProgresoDTO> obtener(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long episodioId,
            @RequestParam Long perfilId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        return ResponseEntity.ok(progresoService.obtener(perfilId, episodioId));
    }

    @Operation(summary = "Listar el progreso de todos los episodios de un anime")
    @GetMapping("/anime/{animeId}")
    public ResponseEntity<List<ProgresoDTO>> porAnime(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long animeId,
            @RequestParam Long perfilId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        return ResponseEntity.ok(progresoService.porAnime(perfilId, animeId));
    }

    @Operation(
            summary = "Obtener la fila 'Continuar viendo'",
            description = """
            Episodios empezados y no terminados, del mas reciente al mas \
            antiguo. Se excluyen los que tienen menos de 30 segundos vistos \
            para filtrar aperturas accidentales.
            """)
    @GetMapping("/continuar")
    public ResponseEntity<List<ContinuarViendoDTO>> continuar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long perfilId,
            @Parameter(description = "Cantidad maxima de elementos", example = "10")
            @RequestParam(defaultValue = "10") int limite) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        return ResponseEntity.ok(progresoService.continuarViendo(perfilId, limite));
    }
}
