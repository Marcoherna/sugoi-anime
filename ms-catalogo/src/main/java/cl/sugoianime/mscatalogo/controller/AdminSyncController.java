package cl.sugoianime.mscatalogo.controller;

import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.CrearAnimeRequest;
import cl.sugoianime.mscatalogo.dto.CrearEpisodioRequest;
import cl.sugoianime.mscatalogo.dto.EpisodioDTO;
import cl.sugoianime.mscatalogo.dto.S3ObjetoDTO;
import cl.sugoianime.mscatalogo.service.AdminCatalogoService;
import cl.sugoianime.mscatalogo.service.JikanSyncService;
import cl.sugoianime.mscatalogo.service.S3StorageService;
import cl.sugoianime.mscatalogo.service.TraduccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Validated
@Tag(name = "Administracion",
        description = "Gestion administrativa de series, capitulos, S3, sincronizacion y traduccion")
@SecurityRequirement(name = "bearerAuth")
public class AdminSyncController {

    private final JikanSyncService jikanSyncService;
    private final TraduccionService traduccionService;
    private final AdminCatalogoService adminCatalogoService;
    private final S3StorageService s3StorageService;

    public AdminSyncController(JikanSyncService jikanSyncService,
                               TraduccionService traduccionService,
                               AdminCatalogoService adminCatalogoService,
                               S3StorageService s3StorageService) {
        this.jikanSyncService = jikanSyncService;
        this.traduccionService = traduccionService;
        this.adminCatalogoService = adminCatalogoService;
        this.s3StorageService = s3StorageService;
    }

    // =================================================================
    // GESTION DE SERIES (ANIMES)
    // =================================================================

    @Operation(summary = "Buscar candidatos en la API de Jikan por nombre")
    @GetMapping("/jikan/buscar")
    public ResponseEntity<List<cl.sugoianime.mscatalogo.dto.jikan.JikanAnimeDTO>> buscarEnJikan(
            @RequestParam("nombre") String nombre) {
        return ResponseEntity.ok(adminCatalogoService.buscarEnJikanPorNombre(nombre));
    }

    @Operation(summary = "Agregar una serie automaticamente por nombre o malId desde Jikan API")
    @PostMapping("/animes/importar")
    public ResponseEntity<AnimeDetalleDTO> importarAnimePorNombre(
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "malId", required = false) Integer malId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCatalogoService.importarDesdeJikan(nombre, malId));
    }

    @Operation(summary = "Crear una nueva serie en el catalogo")
    @PostMapping("/animes")
    public ResponseEntity<AnimeDetalleDTO> crearAnime(
            @Valid @RequestBody CrearAnimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCatalogoService.crearAnime(request));
    }

    @Operation(summary = "Actualizar una serie existente del catalogo")
    @PutMapping("/animes/{id}")
    public ResponseEntity<AnimeDetalleDTO> actualizarAnime(
            @PathVariable Long id,
            @Valid @RequestBody CrearAnimeRequest request) {
        return ResponseEntity.ok(adminCatalogoService.actualizarAnime(id, request));
    }

    @Operation(summary = "Eliminar una serie y todos sus capitulos")
    @DeleteMapping("/animes/{id}")
    public ResponseEntity<Void> eliminarAnime(@PathVariable Long id) {
        adminCatalogoService.eliminarAnime(id);
        return ResponseEntity.noContent().build();
    }

    // =================================================================
    // GESTION DE CAPITULOS (EPISODIOS) Y AWS S3
    // =================================================================

    @Operation(summary = "Autocompletar metadatos de un capitulo desde la API segun serie y numero")
    @GetMapping("/episodios/sugerir")
    public ResponseEntity<AdminCatalogoService.SugerenciaEpisodioDTO> sugerirEpisodio(
            @RequestParam("animeId") Long animeId,
            @RequestParam("numero") Integer numero) {
        return ResponseEntity.ok(adminCatalogoService.sugerirDatosEpisodio(animeId, numero));
    }

    @Operation(summary = "Crear o actualizar un capitulo de una serie")
    @PostMapping("/episodios")
    public ResponseEntity<EpisodioDTO> guardarEpisodio(
            @Valid @RequestBody CrearEpisodioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCatalogoService.guardarEpisodio(request));
    }

    @Operation(summary = "Eliminar un capitulo")
    @DeleteMapping("/episodios/{id}")
    public ResponseEntity<Void> eliminarEpisodio(@PathVariable Long id) {
        adminCatalogoService.eliminarEpisodio(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar los archivos de video disponibles en el bucket AWS S3")
    @GetMapping("/s3/videos")
    public ResponseEntity<List<S3ObjetoDTO>> listarVideosS3() {
        return ResponseEntity.ok(s3StorageService.listarVideos());
    }

    @Operation(summary = "Subir un archivo de video (.mp4) al bucket AWS S3")
    @PostMapping(value = "/s3/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<S3ObjetoDTO> subirVideoS3(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "carpeta", defaultValue = "demo") String carpeta) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(s3StorageService.subirVideo(archivo, carpeta));
    }

    // =================================================================
    // SINCRONIZACION Y TRADUCCION (RF-04, RF-05)
    // =================================================================

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
