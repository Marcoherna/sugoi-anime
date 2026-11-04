package cl.sugoianime.msauth.dto;

import cl.sugoianime.msauth.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Vista administrativa de una cuenta de usuario")
public record AdminUsuarioDTO(
        Long id,
        String email,
        String nombre,
        boolean activo,
        boolean esPremium,
        List<String> roles,
        int cantidadPerfiles,
        OffsetDateTime ultimoAcceso,
        OffsetDateTime creadoEn
) {
    public static AdminUsuarioDTO de(Usuario usuario, int cantidadPerfiles) {
        return new AdminUsuarioDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.isActivo(),
                usuario.isEsPremium(),
                usuario.getRoles().stream().map(Enum::name).sorted().toList(),
                cantidadPerfiles,
                usuario.getUltimoAcceso(),
                usuario.getCreadoEn()
        );
    }
}

