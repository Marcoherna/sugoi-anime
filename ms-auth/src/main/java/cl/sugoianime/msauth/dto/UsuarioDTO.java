package cl.sugoianime.msauth.dto;

import cl.sugoianime.msauth.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Datos publicos de un usuario. Nunca incluye el hash de la contrasena.")
public record UsuarioDTO(

        Long id,

        @Schema(example = "usuario@ejemplo.cl")
        String email,

        @Schema(example = "Ana Perez")
        String nombre,

        @Schema(description = "Indica si la cuenta tiene suscripcion activa", example = "false")
        boolean esPremium,

        @Schema(example = "[\"USUARIO\"]")
        List<String> roles
) {

    public static UsuarioDTO de(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.isEsPremium(),
                usuario.getRoles().stream().map(Enum::name).toList()
        );
    }
}
