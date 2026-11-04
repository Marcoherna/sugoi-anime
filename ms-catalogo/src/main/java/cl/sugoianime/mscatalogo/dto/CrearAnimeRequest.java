package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Datos para crear o actualizar una serie en el catalogo")
public record CrearAnimeRequest(

        @Schema(description = "ID en MyAnimeList (opcional, se autogenera si se omite)", example = "99001")
        Integer malId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 500)
        @Schema(example = "Solo Leveling")
        String titulo,

        @Size(max = 500)
        @Schema(example = "Ore dake Level Up na Ken")
        String tituloJapones,

        @Schema(example = "En un mundo donde cazadores con habilidades magicas deben luchar contra monstruos...")
        String sinopsis,

        @Min(1900) @Max(2100)
        @Schema(example = "2024")
        Integer anio,

        @Min(1)
        @Schema(example = "12")
        Integer episodios,

        @DecimalMin("0.0") @DecimalMax("10.0")
        @Schema(example = "8.65")
        BigDecimal puntuacion,

        @Size(max = 1000)
        @Schema(description = "URL de portada o key en S3", example = "https://cdn.myanimelist.net/images/anime/1801/142390l.jpg")
        String imagenKey,

        @Schema(description = "Estado: EN_EMISION, FINALIZADO, PROXIMAMENTE, DESCONOCIDO", example = "FINALIZADO")
        String estado,

        @Schema(description = "IDs de los generos asociados")
        List<Long> generoIds
) {}

