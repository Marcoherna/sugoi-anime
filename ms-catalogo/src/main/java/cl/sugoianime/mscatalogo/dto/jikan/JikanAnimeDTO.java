package cl.sugoianime.mscatalogo.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanAnimeDTO(

        @JsonProperty("mal_id")         Integer malId,
        @JsonProperty("title")          String title,
        @JsonProperty("title_japanese") String titleJapanese,
        @JsonProperty("synopsis")       String synopsis,
        @JsonProperty("episodes")       Integer episodes,
        @JsonProperty("score")          BigDecimal score,
        @JsonProperty("status")         String status,
        @JsonProperty("images")         JikanImagenDTO images,
        @JsonProperty("aired")          Aired aired,
        @JsonProperty("genres")         List<JikanGeneroDTO> genres
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Aired(@JsonProperty("prop") Prop prop) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Prop(@JsonProperty("from") From from) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record From(@JsonProperty("year") Integer year) {}
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JikanGeneroDTO(
            @JsonProperty("mal_id") Integer malId,
            @JsonProperty("name")   String name
    ) {}

    /** Extrae el anio navegando la estructura anidada con seguridad ante nulos. */
    public Integer anioLanzamiento() {
        if (aired == null || aired.prop() == null || aired.prop().from() == null) {
            return null;
        }
        return aired.prop().from().year();
    }

    public boolean esValido() {
        return malId != null && title != null && !title.isBlank();
    }
}
