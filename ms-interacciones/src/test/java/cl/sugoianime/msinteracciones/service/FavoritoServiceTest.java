package cl.sugoianime.msinteracciones.service;

import cl.sugoianime.msinteracciones.client.CatalogoClient;
import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import cl.sugoianime.msinteracciones.domain.Favorito;
import cl.sugoianime.msinteracciones.exception.InteraccionException;
import cl.sugoianime.msinteracciones.repository.FavoritoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoritoService - reglas de favoritos (RF-06)")
class FavoritoServiceTest {

    @Mock private FavoritoRepository favoritoRepository;
    @Mock private CatalogoClient catalogoClient;

    @InjectMocks private FavoritoService service;

    private AnimeResumenExterno anime() {
        return new AnimeResumenExterno(1L, "Attack on Titan", 2013, null,
                "https://cdn/aot.jpg", List.of());
    }

    @Test
    @DisplayName("rechaza duplicados sin consultar al catalogo")
    void agregar_duplicado_lanza409() {
        when(favoritoRepository.existsByPerfilIdAndAnimeId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.agregar(10L, 1L, 1L))
                .isInstanceOf(InteraccionException.class)
                .hasMessageContaining("ya esta en tus favoritos");

        verify(catalogoClient, never()).obtenerAnime(anyLong());
    }

    @Test
    @DisplayName("valida contra ms-catalogo que el anime exista")
    void agregar_animeInexistente_lanza404() {
        when(favoritoRepository.existsByPerfilIdAndAnimeId(1L, 999L)).thenReturn(false);
        when(catalogoClient.obtenerAnime(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.agregar(10L, 1L, 999L))
                .isInstanceOf(InteraccionException.class)
                .hasMessageContaining("no existe en el catalogo");

        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("si ms-catalogo cae, el listado degrada con datosCompletos=false")
    void listar_catalogoCaido_degradaSinFallar() {
        Favorito favorito = Favorito.builder()
                .id(1L).perfilId(1L).usuarioId(10L).animeId(1L).build();

        when(favoritoRepository.findByPerfilIdOrderByCreadoEnDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(favorito)));
        // Catalogo no disponible: mapa vacio
        when(catalogoClient.obtenerAnimes(any())).thenReturn(Map.of());

        var resultado = service.listar(1L, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().datosCompletos()).isFalse();
        assertThat(resultado.getContent().getFirst().titulo()).isNull();
        // Lo importante: NO lanzo excepcion
    }

    @Test
    @DisplayName("con catalogo disponible enriquece el favorito")
    void listar_enriqueceConDatosDelCatalogo() {
        Favorito favorito = Favorito.builder()
                .id(1L).perfilId(1L).usuarioId(10L).animeId(1L).build();

        when(favoritoRepository.findByPerfilIdOrderByCreadoEnDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(favorito)));
        when(catalogoClient.obtenerAnimes(any())).thenReturn(Map.of(1L, anime()));

        var resultado = service.listar(1L, PageRequest.of(0, 20));

        assertThat(resultado.getContent().getFirst().titulo()).isEqualTo("Attack on Titan");
        assertThat(resultado.getContent().getFirst().datosCompletos()).isTrue();
    }
}
