package cl.sugoianime.mscatalogo.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanPaginaResponse(
        @JsonProperty("data")       List<JikanAnimeDTO> data,
        @JsonProperty("pagination") Paginacion pagination
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paginacion(
            @JsonProperty("last_visible_page") Integer lastVisiblePage,
            @JsonProperty("has_next_page")     Boolean hasNextPage
    ) {}

    public List<JikanAnimeDTO> datosSeguro() {
        return data == null ? Collections.emptyList() : data;
    }

    public boolean hayMasPaginas() {
        return pagination != null && Boolean.TRUE.equals(pagination.hasNextPage());
    }
}
