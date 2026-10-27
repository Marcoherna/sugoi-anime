package cl.sugoianime.msinteracciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Actualizacion del punto de reproduccion (RF-03)")
public record GuardarProgresoRequest(

        @NotNull(message = "El perfil es obligatorio")
        @Schema(example = "1")
        Long perfilId,

        @NotNull(message = "El anime es obligatorio")
        @Schema(example = "1")
        Long animeId,

        @NotNull(message = "El episodio es obligatorio")
        @Schema(example = "1")
        Long episodioId,

        @Schema(example = "1")
        Integer numeroEpisodio,

        @NotNull(message = "El segundo es obligatorio")
        @Min(value = 0, message = "El segundo no puede ser negativo")
        @Schema(description = "Segundo exacto de reproduccion", example = "742")
        Integer segundo,

        @Min(0)
        @Schema(description = "Duracion total del episodio", example = "1440")
        Integer duracionSeg
) {}
