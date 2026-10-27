package cl.sugoianime.msinteracciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Progreso de reproduccion de un episodio (RF-03)")
public record ProgresoDTO(

        Long animeId,

        Long episodioId,

        Integer numeroEpisodio,

        @Schema(description = "Segundo exacto donde quedo la reproduccion", example = "742")
        Integer segundo,

        @Schema(description = "Duracion total del episodio en segundos", example = "1440")
        Integer duracionSeg,

        @Schema(description = "Porcentaje visto, de 0 a 100", example = "51")
        int porcentaje,

        @Schema(description = "Se marca como completado al superar el 90%", example = "false")
        boolean completado,

        OffsetDateTime actualizadoEn
) {}
