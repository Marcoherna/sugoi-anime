package cl.sugoianime.mscatalogo.controller;

import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.AnimeResumenDTO;
import cl.sugoianime.mscatalogo.dto.GeneroDTO;
import cl.sugoianime.mscatalogo.dto.PaginaDTO;
import cl.sugoianime.mscatalogo.exception.ErrorResponse;
import cl.sugoianime.mscatalogo.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/animes")
@Validated
@Tag(name = "Catalogo",
        description = "Navegacion, busqueda y detalle del catalogo de anime (RF-01)")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Operation(
            summary = "Listar el catalogo paginado con filtros",
            description = """
            Implementa el requerimiento **RF-01**.

            Devuelve el catalogo paginado. Los tres filtros son opcionales y \
            se combinan con AND: si no envias ninguno, obtienes el catalogo completo.

            La paginacion usa los parametros estandar de Spring Data: \
            `page` (base 0), `size` y `sort` (ej: `sort=puntuacion,desc`).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Pagina de resultados obtenida correctamente"),
            @ApiResponse(responseCode = "400",
                    description = "Parametros de filtro o paginacion invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PaginaDTO<AnimeResumenDTO>> listar(

            @Parameter(description = "Nombre del genero, en espanol o ingles",
                    example = "Accion")
            @RequestParam(required = false) String genero,

            @Parameter(description = "Anio exacto de lanzamiento", example = "2013")
            @RequestParam(required = false)
            @Min(1900) @Max(2100) Integer anio,

            @Parameter(description = "Texto a buscar en el titulo", example = "titan")
            @RequestParam(required = false) String texto,

            @PageableDefault(size = 20, sort = "puntuacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(catalogoService.buscar(genero, anio, texto, pageable));
    }

    @Operation(
            summary = "Obtener el detalle completo de un anime",
            description = """
            Devuelve la ficha completa incluyendo sinopsis (traducida al espanol \
            cuando existe, segun RF-05), generos y episodios disponibles para \
            reproduccion.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anime encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un anime con ese id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AnimeDetalleDTO> detalle(
            @Parameter(description = "Identificador interno del anime", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(catalogoService.obtenerDetalle(id));
    }

    @Operation(
            summary = "Listar los generos disponibles",
            description = "Alimenta el selector de filtros del frontend (RF-01).")
    @ApiResponse(responseCode = "200", description = "Listado de generos")
    @GetMapping("/generos")
    public ResponseEntity<List<GeneroDTO>> generos() {
        return ResponseEntity.ok(catalogoService.listarGeneros());
    }

    @Operation(
            summary = "Listar los animes mejor valorados",
            description = "Fila destacada del home, ordenada por puntuacion descendente.")
    @GetMapping("/top")
    public ResponseEntity<PaginaDTO<AnimeResumenDTO>> top(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(catalogoService.topValorados(pageable));
    }
}
