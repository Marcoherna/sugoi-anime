package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Vista resumida de un anime, usada en el catalogo paginado (RF-01)")
public record AnimeResumenDTO(

        @Schema(description = "Identificador interno", example = "1")
        Long id,

        @Schema(description = "Titulo principal", example = "Attack on Titan")
        String titulo,

        @Schema(description = "Anio de lanzamiento", example = "2013")
        Integer anio,

        @Schema(description = "Puntuacion promedio de 0 a 10", example = "8.54")
        BigDecimal puntuacion,

        @Schema(description = "URL completa de la portada, servida por el CDN (RNF-07)",
                example = "https://d123.cloudfront.net/portadas/aot.jpg")
        String imagenUrl,

        @Schema(description = "Generos asociados")
        List<GeneroDTO> generos
) {}