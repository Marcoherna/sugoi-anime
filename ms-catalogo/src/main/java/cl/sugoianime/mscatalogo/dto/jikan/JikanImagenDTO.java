package cl.sugoianime.mscatalogo.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Estructura de imagenes que devuelve Jikan.
 *
 * NOTA JACKSON 3: el paquete de las anotaciones NO cambia en Spring Boot 4.
 * Sigue siendo com.fasterxml.jackson.annotation. Solo databind y core
 * se movieron a tools.jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanImagenDTO(
        @JsonProperty("jpg") Formato jpg
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Formato(
            @JsonProperty("image_url")       String imageUrl,
            @JsonProperty("large_image_url") String largeImageUrl
    ) {}

    public String mejorUrl() {
        if (jpg == null) return null;
        return jpg.largeImageUrl() != null ? jpg.largeImageUrl() : jpg.imageUrl();
    }
}
