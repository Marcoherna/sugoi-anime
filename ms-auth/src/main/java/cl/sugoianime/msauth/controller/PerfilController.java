package cl.sugoianime.msauth.controller;

import cl.sugoianime.msauth.dto.CrearPerfilRequest;
import cl.sugoianime.msauth.dto.PerfilDTO;
import cl.sugoianime.msauth.exception.ErrorResponse;
import cl.sugoianime.msauth.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/perfiles")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Perfiles",
        description = "Perfiles de visualizacion bajo una cuenta matriz (RF-09)")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @Operation(
            summary = "Listar los perfiles de la cuenta autenticada",
            description = """
            Implementa el requerimiento **RF-09**.

            El usuario se obtiene del claim `sub` del JWT, nunca de un \
            parametro: asi es imposible pedir los perfiles de otra cuenta.
            """)
    @GetMapping
    public ResponseEntity<List<PerfilDTO>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(perfilService.listar(usuarioId(jwt)));
    }

    @Operation(summary = "Crear un perfil nuevo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado"),
            @ApiResponse(responseCode = "409",
                    description = "Nombre duplicado o limite de perfiles alcanzado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PerfilDTO> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CrearPerfilRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilService.crear(usuarioId(jwt), request));
    }

    @Operation(summary = "Actualizar un perfil propio")
    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador del perfil") @PathVariable Long id,
            @Valid @RequestBody CrearPerfilRequest request) {

        return ResponseEntity.ok(perfilService.actualizar(usuarioId(jwt), id, request));
    }

    @Operation(
            summary = "Eliminar un perfil propio",
            description = "No se permite eliminar el ultimo perfil de la cuenta.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        perfilService.eliminar(usuarioId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
