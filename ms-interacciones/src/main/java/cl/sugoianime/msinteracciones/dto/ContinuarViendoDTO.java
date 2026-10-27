package cl.sugoianime.msinteracciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Elemento de la fila 'Continuar viendo' del home")
public record ContinuarViendoDTO(

        Long animeId,

        @Schema(example = "Attack on Titan")
        String titulo,

        String imagenUrl,

        Long episodioId,

        Integer numeroEpisodio,

        @Schema(description = "Segundo desde donde debe reanudarse", example = "742")
        Integer segundo,

        @Schema(example = "51")
        int porcentaje
) {}
