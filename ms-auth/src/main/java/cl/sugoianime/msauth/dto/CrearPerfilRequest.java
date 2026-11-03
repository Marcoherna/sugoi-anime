package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o modificar un perfil de visualizacion")
public record CrearPerfilRequest(

        @NotBlank(message = "El nombre del perfil es obligatorio")
        @Size(min = 1, max = 50)
        @Schema(example = "Ana")
        String nombre,

        @Pattern(regexp = "^[a-z0-9-]{1,50}$",
                message = "El avatar solo admite minusculas, numeros y guiones")
        @Schema(example = "avatar-3")
        String avatar,

        @Schema(example = "false")
        Boolean esInfantil
) {}
