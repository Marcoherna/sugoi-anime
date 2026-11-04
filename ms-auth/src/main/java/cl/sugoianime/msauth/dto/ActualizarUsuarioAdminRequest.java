package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Datos modificables por un administrador sobre una cuenta")
public record ActualizarUsuarioAdminRequest(
        @Schema(description = "Estado activo/bloqueado de la cuenta")
        Boolean activo,

        @Schema(description = "Indica si la cuenta posee suscripcion Premium")
        Boolean esPremium,

        @Schema(description = "Lista de roles asignados (USUARIO, PREMIUM, ADMIN)")
        List<String> roles
) {}

