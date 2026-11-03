package cl.sugoianime.msauth.dto;

import cl.sugoianime.msauth.domain.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil de visualizacion (RF-09)")
public record PerfilDTO(

        Long id,

        @Schema(example = "Ana")
        String nombre,

        @Schema(example = "avatar-3")
        String avatar,

        @Schema(description = "Si es true, se aplican restricciones de contenido por edad",
                example = "false")
        boolean esInfantil
) {

    public static PerfilDTO de(Perfil perfil) {
        return new PerfilDTO(perfil.getId(), perfil.getNombre(),
                perfil.getAvatar(), perfil.isEsInfantil());
    }
}
