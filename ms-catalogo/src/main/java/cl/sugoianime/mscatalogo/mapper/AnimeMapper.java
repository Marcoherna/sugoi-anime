package cl.sugoianime.mscatalogo.mapper;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.Episodio;
import cl.sugoianime.mscatalogo.domain.Genero;
import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.AnimeResumenDTO;
import cl.sugoianime.mscatalogo.dto.EpisodioDTO;
import cl.sugoianime.mscatalogo.dto.GeneroDTO;
import cl.sugoianime.mscatalogo.service.StreamingService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Convierte entidades JPA a DTOs.
 *
 * Mapeo manual a proposito: es explicito, depurable y sin magia en tiempo
 * de compilacion. Con solo 3 entidades no se justifica MapStruct.
 */
@Component
public class AnimeMapper {

    private final StreamingService streamingService;

    public AnimeMapper(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    public GeneroDTO aGeneroDTO(Genero genero) {
        return new GeneroDTO(genero.getId(), genero.nombreParaMostrar());
    }

    public EpisodioDTO aEpisodioDTO(Episodio episodio) {
        return new EpisodioDTO(
                episodio.getId(),
                episodio.getNumero(),
                episodio.getTitulo(),
                episodio.getDuracionSeg(),
                episodio.getLicencia()
        );
    }

    public AnimeResumenDTO aResumenDTO(Anime anime) {
        return new AnimeResumenDTO(
                anime.getId(),
                anime.getTitulo(),
                anime.getAnio(),
                anime.getPuntuacion(),
                streamingService.construirUrlPublica(anime.getImagenKey()),
                mapearGeneros(anime.getGeneros())
        );
    }

    public AnimeDetalleDTO aDetalleDTO(Anime anime) {
        boolean traducida = anime.getSinopsisEs() != null && !anime.getSinopsisEs().isBlank();

        return new AnimeDetalleDTO(
                anime.getId(),
                anime.getTitulo(),
                anime.getTituloJapones(),
                traducida ? anime.getSinopsisEs() : anime.getSinopsis(),
                traducida,
                anime.getAnio(),
                anime.getEpisodios(),
                anime.getPuntuacion(),
                streamingService.construirUrlPublica(anime.getImagenKey()),
                anime.getEstado().name(),
                mapearGeneros(anime.getGeneros()),
                anime.getEpisodiosLista().stream()
                        .sorted(Comparator.comparing(Episodio::getNumero))
                        .map(this::aEpisodioDTO)
                        .toList()
        );
    }

    private List<GeneroDTO> mapearGeneros(Set<Genero> generos) {
        return generos.stream()
                .sorted(Comparator.comparing(Genero::nombreParaMostrar))
                .map(this::aGeneroDTO)
                .toList();
    }
}
