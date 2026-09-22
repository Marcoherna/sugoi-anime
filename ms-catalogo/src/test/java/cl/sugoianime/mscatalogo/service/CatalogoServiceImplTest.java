package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.EstadoAnime;
import cl.sugoianime.mscatalogo.dto.AnimeResumenDTO;
import cl.sugoianime.mscatalogo.dto.PaginaDTO;
import cl.sugoianime.mscatalogo.exception.RecursoNoEncontradoException;
import cl.sugoianime.mscatalogo.mapper.AnimeMapper;
import cl.sugoianime.mscatalogo.repository.AnimeRepository;
import cl.sugoianime.mscatalogo.repository.GeneroRepository;
import cl.sugoianime.mscatalogo.service.impl.CatalogoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoService - logica de busqueda del catalogo (RF-01)")
class CatalogoServiceImplTest {

    @Mock private AnimeRepository animeRepository;
    @Mock private GeneroRepository generoRepository;
    @Mock private AnimeMapper mapper;

    @InjectMocks private CatalogoServiceImpl service;

    private Anime animeDePrueba() {
        return Anime.builder()
                .id(1L)
                .malId(16498)
                .titulo("Attack on Titan")
                .anio(2013)
                .puntuacion(new BigDecimal("8.54"))
                .estado(EstadoAnime.FINALIZADO)
                .build();
    }

    private AnimeResumenDTO dtoDePrueba() {
        return new AnimeResumenDTO(1L, "Attack on Titan", 2013,
                new BigDecimal("8.54"), null, List.of());
    }

    @Test
    @DisplayName("devuelve una pagina con el contenido mapeado a DTO")
    void buscar_devuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Anime> pagina = new PageImpl<>(List.of(animeDePrueba()), pageable, 1);

        when(animeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        when(mapper.aResumenDTO(any(Anime.class))).thenReturn(dtoDePrueba());

        PaginaDTO<AnimeResumenDTO> resultado =
                service.buscar("Accion", 2013, null, pageable);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().getFirst().titulo()).isEqualTo("Attack on Titan");
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.pagina()).isZero();
        assertThat(resultado.ultima()).isTrue();
    }

    @Test
    @DisplayName("sin filtros consulta igualmente el repositorio")
    void buscar_sinFiltros_consultaElRepositorio() {
        Pageable pageable = PageRequest.of(0, 20);
        when(animeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PaginaDTO<AnimeResumenDTO> resultado =
                service.buscar(null, null, null, pageable);

        assertThat(resultado.contenido()).isEmpty();
        verify(animeRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("obtenerDetalle lanza 404 cuando el anime no existe")
    void obtenerDetalle_noExiste_lanzaExcepcion() {
        when(animeRepository.findWithDetalleById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetalle(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("listarGeneros delega en el repositorio de generos")
    void listarGeneros_delegaEnRepositorio() {
        when(generoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertThat(service.listarGeneros()).isEmpty();
        verify(generoRepository).findAllByOrderByNombreAsc();
    }
}