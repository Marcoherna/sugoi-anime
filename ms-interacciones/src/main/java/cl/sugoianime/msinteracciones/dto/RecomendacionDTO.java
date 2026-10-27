package cl.sugoianime.msinteracciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Anime recomendado para un perfil (RF-07)")
public record RecomendacionDTO(

        Long animeId,

        @Schema(example = "Fullmetal Alchemist: Brotherhood")
        String titulo,

        Integer anio,

        BigDecimal puntuacion,

        String imagenUrl,

        @Schema(description = "Similitud de coseno con el perfil de gustos, de 0 a 1",
                example = "0.8165")
        Double puntajeSimilitud,

        @Schema(description = "Generos que explican la recomendacion",
                example = "[\"Accion\", \"Drama\"]")
        List<String> generosCoincidentes
) {}
