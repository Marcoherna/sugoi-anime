package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar una cuenta nueva (RF-08)")
public record RegistroRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es valido")
        @Size(max = 255)
        @Schema(example = "usuario@ejemplo.cl")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 10, max = 72,
                message = "La contrasena debe tener entre 10 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contrasena debe incluir al menos una minuscula, una mayuscula y un numero")
        @Schema(example = "MiClaveSegura123")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 150)
        @Schema(example = "Ana Perez")
        String nombre,

        @AssertTrue(message = "Debe aceptar los terminos y condiciones para registrarse")
        @Schema(description = """
        Consentimiento explicito exigido por la Ley 19.628. \
        Se registra de forma auditable en la tabla consentimiento.
        """,
                example = "true")
        Boolean aceptaTerminos,

        @Schema(description = """
        Consentimiento separado para usar el historial de visualizacion \
        en el motor de recomendaciones (RF-07). Es opcional: el usuario \
        puede registrarse sin aceptarlo.
        """,
                example = "true")
        Boolean aceptaRecomendaciones
) {}
