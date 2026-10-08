package cl.sugoianime.mscatalogo.controller;

import cl.sugoianime.mscatalogo.service.JikanSyncService;
import cl.sugoianime.mscatalogo.service.TraduccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Validated
@Tag(name = "Administracion",
        description = "Operaciones de sincronizacion y mantenimiento (RF-04, RF-05)")
@SecurityRequirement(name = "bearerAuth")
public class AdminSyncController {

    private final JikanSyncService jikanSyncService;
    private final TraduccionService traduccionService;

    public AdminSyncController(JikanSyncService jikanSyncService,
                               TraduccionService traduccionService) {
        this.jikanSyncService = jikanSyncService;
        this.traduccionService = traduccionService;
    }

    @Operation(
            summary = "Disparar la sincronizacion con Jikan manualmente",
            description = """
            Implementa el requerimiento **RF-04** en su modalidad manual \
            (rol administrador). La modalidad automatizada corre como job \
            programado a las 03:00.

            Este endpoint respeta el rate limit de Jikan con una pausa entre \
            llamadas y esta protegido por un circuit breaker (riesgo R02).
            """)
    @PostMapping("/sincronizar")
    public ResponseEntity<JikanSyncService.ResultadoSync> sincronizar(
            @Parameter(description = "Cantidad de paginas a consultar (25 animes por pagina)",
                    example = "2")
            @RequestParam(defaultValue = "2")
            @Min(1) @Max(10) int paginas) {

        return ResponseEntity.ok(jikanSyncService.sincronizar(paginas));
    }

    @Operation(
            summary = "Disparar la traduccion de sinopsis pendientes",
            description = "Implementa el requerimiento **RF-05** en modalidad manual.")
    @PostMapping("/traducir")
    public ResponseEntity<String> traducir() {
        traduccionService.traducirPendientes();
        return ResponseEntity.ok("Proceso de traduccion ejecutado. Revisa los logs.");
    }
}
