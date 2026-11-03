package cl.sugoianime.msauth.controller;

import cl.sugoianime.msauth.dto.*;
import cl.sugoianime.msauth.exception.ErrorResponse;
import cl.sugoianime.msauth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion",
        description = "Registro, inicio y cierre de sesion (RF-08, RNF-05)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Registrar una cuenta nueva",
            description = """
            Implementa el requerimiento **RF-08**.

            Crea la cuenta, genera un perfil de visualizacion por defecto \
            (RF-09) y devuelve los tokens de sesion.

            **Ley 19.628.** El campo `aceptaTerminos` es obligatorio y su \
            aceptacion se persiste de forma auditable junto con la version \
            de los terminos, la fecha y la IP de origen. El consentimiento \
            para el uso del historial en recomendaciones se registra por \
            separado y es opcional.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o terminos no aceptados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El email ya esta registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/registro")
    public ResponseEntity<TokenResponse> registrar(
            @Valid @RequestBody RegistroRequest request,
            HttpServletRequest http) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrar(request, ipDe(http)));
    }

    @Operation(
            summary = "Iniciar sesion",
            description = """
            Devuelve un `accessToken` (JWT, 15 minutos) y un `refreshToken` \
            (opaco, 7 dias).

            El `accessToken` se envia en cada peticion como \
            `Authorization: Bearer <token>` y lo valida el API Gateway (RNF-04, RNF-05).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticacion exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La cuenta esta desactivada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Renovar el access token",
            description = """
            Entrega un access token nuevo a partir de un refresh token valido, \
            sin pedir credenciales otra vez.

            El refresh token se **rota**: el usado queda revocado y se emite \
            uno nuevo. Asi, un token robado deja de servir en cuanto el \
            titular legitimo renueva su sesion.
            """)
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refrescar(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refrescar(request.refreshToken()));
    }

    @Operation(
            summary = "Cerrar sesion en el dispositivo actual",
            description = """
            Revoca el refresh token entregado. El access token sigue siendo \
            valido hasta que expire (maximo 15 minutos): esa es una \
            caracteristica inherente a los JWT sin estado, y la razon de \
            mantener su vigencia corta.
            """)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cerrar sesion en todos los dispositivos")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-todos")
    public ResponseEntity<Void> logoutTodos(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutTodosLosDispositivos(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener los datos de la cuenta autenticada")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/yo")
    public ResponseEntity<UsuarioDTO> yo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                authService.obtenerPerfilPropio(Long.valueOf(jwt.getSubject())));
    }

    @Operation(
            summary = "Eliminar la cuenta y todos sus datos",
            description = """
            **Derecho de eliminacion, Ley 19.628.**

            Borra de forma permanente la cuenta, sus perfiles, sus \
            consentimientos y sus tokens. La operacion es irreversible.
            """)
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/cuenta")
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal Jwt jwt) {
        authService.eliminarCuenta(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    /** Obtiene la IP real considerando que el Gateway actua como proxy. */
    private String ipDe(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
