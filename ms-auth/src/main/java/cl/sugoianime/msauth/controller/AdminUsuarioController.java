package cl.sugoianime.msauth.controller;

import cl.sugoianime.msauth.dto.ActualizarUsuarioAdminRequest;
import cl.sugoianime.msauth.dto.AdminUsuarioDTO;
import cl.sugoianime.msauth.service.AdminUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administracion de Cuentas",
        description = "Gestion de cuentas, roles y estado por administradores")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @Operation(summary = "Listar todas las cuentas registradas")
    @GetMapping
    public ResponseEntity<List<AdminUsuarioDTO>> listar() {
        return ResponseEntity.ok(adminUsuarioService.listarTodos());
    }

    @Operation(summary = "Actualizar estado activo, suscripcion Premium o roles de una cuenta")
    @PutMapping("/{id}")
    public ResponseEntity<AdminUsuarioDTO> actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody ActualizarUsuarioAdminRequest request) {

        Long adminId = jwt == null ? null : Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(adminUsuarioService.actualizar(adminId, id, request));
    }

    @Operation(summary = "Eliminar una cuenta de usuario")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        Long adminId = jwt == null ? null : Long.valueOf(jwt.getSubject());
        adminUsuarioService.eliminar(adminId, id);
        return ResponseEntity.noContent().build();
    }
}

