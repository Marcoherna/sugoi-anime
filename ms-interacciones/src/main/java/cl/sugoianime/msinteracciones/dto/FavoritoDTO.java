package cl.sugoianime.msinteracciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Anime marcado como favorito, enriquecido con datos del catalogo")
public record FavoritoDTO(

        Long favoritoId,

        Long animeId,

        @Schema(example = "Attack on Titan")
        String titulo,

        Integer anio,

        BigDecimal puntuacion,

        @Schema(description = "URL de la portada servida por el CDN")
        String imagenUrl,

        OffsetDateTime agregadoEn,

        @Schema(description = """
        Indica si se pudieron obtener los datos del catalogo. Si ms-catalogo \
        esta caido, el favorito se devuelve igual pero con este campo en false \
        y los datos descriptivos vacios.
        """,
                example = "true")
        boolean datosCompletos
) {}
