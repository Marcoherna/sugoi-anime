package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar o actualizar un capitulo (Ley 17.336)")
public record CrearEpisodioRequest(

        @NotNull(message = "El ID del anime es obligatorio")
        Long animeId,

        @NotNull(message = "El numero de episodio es obligatorio")
        @Min(value = 1, message = "El numero de episodio debe ser mayor a 0")
        Integer numero,

        @NotBlank(message = "El titulo del episodio es obligatorio")
        @Size(max = 500)
        String titulo,

        @NotNull(message = "La duracion en segundos es obligatoria")
        @Min(value = 1, message = "La duracion debe ser mayor a 0 segundos")
        Integer duracionSeg,

        @NotBlank(message = "La key de S3 o URL del video es obligatoria")
        @Size(max = 1000)
        String videoKey,

        @NotBlank(message = "La licencia del contenido es obligatoria (Ley 17.336)")
        @Size(max = 200)
        @Schema(example = "CC-BY-3.0")
        String licencia,

        @Size(max = 1000)
        @Schema(example = "https://peach.blender.org/download/")
        String fuenteUrl
) {}

