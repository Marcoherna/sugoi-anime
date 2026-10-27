package cl.sugoianime.msinteracciones.controller;

import cl.sugoianime.msinteracciones.dto.FavoritoDTO;
import cl.sugoianime.msinteracciones.exception.ErrorResponse;
import cl.sugoianime.msinteracciones.security.PerfilGuard;
import cl.sugoianime.msinteracciones.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/favoritos")
@Tag(name = "Favoritos", description = "Animes marcados como favoritos por perfil (RF-06)")
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final PerfilGuard perfilGuard;

    public FavoritoController(FavoritoService favoritoService, PerfilGuard perfilGuard) {
        this.favoritoService = favoritoService;
        this.perfilGuard = perfilGuard;
    }

    @Operation(
            summary = "Listar los favoritos de un perfil",
            description = """
            Implementa el requerimiento **RF-06**.

            Los favoritos se enriquecen con titulo, portada y puntuacion \
            consultando a ms-catalogo. Si ese servicio no esta disponible, \
            la respuesta se devuelve igualmente con `datosCompletos: false`.
            """)
    @GetMapping
    public ResponseEntity<Page<FavoritoDTO>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Perfil cuyos favoritos se consultan", example = "1")
            @RequestParam Long perfilId,
            @PageableDefault(size = 20) Pageable pageable) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        return ResponseEntity.ok(favoritoService.listar(perfilId, pageable));
    }

    @Operation(
            summary = "Marcar un anime como favorito",
            description = """
            Verifica contra ms-catalogo que el anime exista antes de guardar. \
            Esa comprobacion sustituye a la clave foranea que no puede existir \
            entre bases de datos separadas (RNF-02).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Favorito agregado"),
            @ApiResponse(responseCode = "404", description = "El anime o el perfil no existen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya estaba en favoritos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FavoritoDTO> agregar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long perfilId,
            @RequestParam Long animeId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoritoService.agregar(perfilGuard.usuarioIdDe(jwt), perfilId, animeId));
    }

    @Operation(summary = "Quitar un anime de favoritos")
    @DeleteMapping("/{animeId}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long animeId,
            @RequestParam Long perfilId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);
        favoritoService.eliminar(perfilId, animeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Consultar si un anime esta en favoritos",
            description = "Lo usa el frontend para pintar el corazon lleno o vacio.")
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long perfilId,
            @RequestParam Long animeId) {

        perfilGuard.verificarPropiedad(jwt, perfilId);

        return ResponseEntity.ok(Map.of(
                "animeId", animeId,
                "esFavorito", favoritoService.esFavorito(perfilId, animeId),
                "totalFavoritos", favoritoService.contar(perfilId)));
    }
}
