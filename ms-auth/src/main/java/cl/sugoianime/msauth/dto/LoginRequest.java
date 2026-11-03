package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesion (RF-08)")
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Schema(example = "usuario@ejemplo.cl")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Schema(example = "MiClaveSegura123")
        String password
) {}
