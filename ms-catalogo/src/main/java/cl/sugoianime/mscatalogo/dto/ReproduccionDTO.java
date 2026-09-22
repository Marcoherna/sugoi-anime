package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos necesarios para iniciar la reproduccion de un episodio (RF-02)")
public record ReproduccionDTO(

        Long episodioId,

        Long animeId,

        @Schema(example = "Attack on Titan")
        String tituloAnime,

        @Schema(example = "1")
        Integer numeroEpisodio,

        @Schema(description = "URL del video servida por el CDN (RNF-07)",
                example = "https://d123.cloudfront.net/demo/big_buck_bunny_1080p.mp4")
        String urlVideo,

        @Schema(description = "Duracion total en segundos", example = "634")
        Integer duracionSeg,

        @Schema(description = "Licencia del contenido, requerida por la Ley 17.336",
                example = "CC-BY-3.0")
        String licencia
) {}
