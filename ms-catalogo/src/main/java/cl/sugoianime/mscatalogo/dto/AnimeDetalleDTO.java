package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Vista completa de un anime, con episodios reproducibles")
public record AnimeDetalleDTO(

        Long id,

        @Schema(example = "Attack on Titan")
        String titulo,

        @Schema(example = "Shingeki no Kyojin")
        String tituloJapones,

        @Schema(description = "Sinopsis en espanol si existe traduccion, si no la original (RF-05)")
        String sinopsis,

        @Schema(description = "Indica si la sinopsis mostrada esta traducida", example = "true")
        boolean sinopsisTraducida,

        Integer anio,

        @Schema(description = "Cantidad total de episodios de la serie", example = "25")
        Integer episodios,

        BigDecimal puntuacion,

        String imagenUrl,

        @Schema(example = "FINALIZADO")
        String estado,

        List<GeneroDTO> generos,

        @Schema(description = "Episodios disponibles en la plataforma")
        List<EpisodioDTO> episodiosDisponibles
) {}