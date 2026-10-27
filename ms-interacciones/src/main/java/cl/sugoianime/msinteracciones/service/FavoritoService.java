package cl.sugoianime.msinteracciones.service;

import cl.sugoianime.msinteracciones.client.CatalogoClient;
import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import cl.sugoianime.msinteracciones.domain.Favorito;
import cl.sugoianime.msinteracciones.dto.FavoritoDTO;
import cl.sugoianime.msinteracciones.exception.InteraccionException;
import cl.sugoianime.msinteracciones.repository.FavoritoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoritoService {

    private static final Logger log = LoggerFactory.getLogger(FavoritoService.class);

    private final FavoritoRepository favoritoRepository;
    private final CatalogoClient catalogoClient;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           CatalogoClient catalogoClient) {
        this.favoritoRepository = favoritoRepository;
        this.catalogoClient = catalogoClient;
    }

    @Transactional
    public FavoritoDTO agregar(Long usuarioId, Long perfilId, Long animeId) {

        if (favoritoRepository.existsByPerfilIdAndAnimeId(perfilId, animeId)) {
            throw InteraccionException.favoritoDuplicado();
        }

        // Validacion de integridad referencial entre servicios:
        // sin FK, es la aplicacion quien debe comprobar que el anime existe.
        AnimeResumenExterno anime = catalogoClient.obtenerAnime(animeId);
        if (anime == null) {
            throw InteraccionException.animeNoExiste(animeId);
        }

        Favorito favorito = favoritoRepository.save(Favorito.builder()
                .perfilId(perfilId)
                .usuarioId(usuarioId)
                .animeId(animeId)
                .build());

        log.debug("Favorito agregado: perfil={}, anime={}", perfilId, animeId);

        return mapear(favorito, anime);
    }

    @Transactional
    public void eliminar(Long perfilId, Long animeId) {
        Favorito favorito = favoritoRepository.findByPerfilIdAndAnimeId(perfilId, animeId)
                .orElseThrow(InteraccionException::favoritoNoEncontrado);

        favoritoRepository.delete(favorito);
    }

    @Transactional(readOnly = true)
    public Page<FavoritoDTO> listar(Long perfilId, Pageable pageable) {

        Page<Favorito> pagina =
                favoritoRepository.findByPerfilIdOrderByCreadoEnDesc(perfilId, pageable);

        // Enriquecimiento con datos del catalogo. Si ms-catalogo no
        // responde, se devuelven los favoritos con datosCompletos=false
        // en vez de fallar la peticion completa.
        List<Long> ids = pagina.getContent().stream().map(Favorito::getAnimeId).toList();
        var animes = catalogoClient.obtenerAnimes(ids);

        return pagina.map(f -> mapear(f, animes.get(f.getAnimeId())));
    }

    @Transactional(readOnly = true)
    public boolean esFavorito(Long perfilId, Long animeId) {
        return favoritoRepository.existsByPerfilIdAndAnimeId(perfilId, animeId);
    }

    @Transactional(readOnly = true)
    public long contar(Long perfilId) {
        return favoritoRepository.countByPerfilId(perfilId);
    }

    private FavoritoDTO mapear(Favorito favorito, AnimeResumenExterno anime) {
        if (anime == null) {
            return new FavoritoDTO(
                    favorito.getId(), favorito.getAnimeId(),
                    null, null, null, null,
                    favorito.getCreadoEn(), false);
        }
        return new FavoritoDTO(
                favorito.getId(), favorito.getAnimeId(),
                anime.titulo(), anime.anio(), anime.puntuacion(), anime.imagenUrl(),
                favorito.getCreadoEn(), true);
    }
}
