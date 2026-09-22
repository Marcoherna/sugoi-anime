package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Episodio disponible para reproduccion")
public record EpisodioDTO(

        @Schema(description = "Identificador del episodio", example = "42")
        Long id,

        @Schema(description = "Numero de episodio dentro de la serie", example = "1")
        Integer numero,

        @Schema(description = "Titulo del episodio", example = "El comienzo")
        String titulo,

        @Schema(description = "Duracion en segundos", example = "1440")
        Integer duracionSeg,

        @Schema(description = "Licencia del contenido (trazabilidad Ley 17.336)",
                example = "CC-BY-3.0")
        String licencia
) {}