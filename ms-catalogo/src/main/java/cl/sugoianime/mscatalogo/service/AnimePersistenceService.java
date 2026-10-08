package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.EstadoAnime;
import cl.sugoianime.mscatalogo.domain.Genero;
import cl.sugoianime.mscatalogo.dto.jikan.JikanAnimeDTO;
import cl.sugoianime.mscatalogo.repository.AnimeRepository;
import cl.sugoianime.mscatalogo.repository.GeneroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistencia del catalogo sincronizado desde Jikan (RF-04).
 *
 * POR QUE ESTA EN UNA CLASE APARTE Y NO DENTRO DE JikanSyncService:
 * Spring aplica @Transactional mediante proxies. Una llamada entre metodos
 * de la MISMA clase usa "this" y no pasa por el proxy, asi que la
 * transaccion nunca se abre. Al estar en un bean distinto, la llamada
 * atraviesa el proxy y la transaccion funciona: sin esto, cargar la
 * coleccion LAZY "generos" lanza LazyInitializationException.
 */
@Service
public class AnimePersistenceService {

    private final AnimeRepository animeRepository;
    private final GeneroRepository generoRepository;

    public AnimePersistenceService(AnimeRepository animeRepository,
                                   GeneroRepository generoRepository) {
        this.animeRepository = animeRepository;
        this.generoRepository = generoRepository;
    }

    /**
     * Inserta o actualiza un anime segun su mal_id.
     *
     * @return true si el anime era nuevo, false si ya existia
     */
    @Transactional
    public boolean guardarOActualizar(JikanAnimeDTO dto) {
        Optional<Anime> existente = animeRepository.findByMalId(dto.malId());
        boolean esNuevo = existente.isEmpty();

        Anime anime = existente.orElseGet(
                () -> Anime.builder().malId(dto.malId()).build());

        anime.setTitulo(dto.title());
        anime.setTituloJapones(dto.titleJapanese());
        anime.setSinopsis(dto.synopsis());
        anime.setEpisodios(dto.episodes());
        anime.setPuntuacion(dto.score());
        anime.setAnio(dto.anioLanzamiento());
        anime.setEstado(EstadoAnime.desdeJikan(dto.status()));

        // Solo tomamos la imagen externa si aun no tenemos una propia en S3.
        if (anime.getImagenKey() == null && dto.images() != null) {
            anime.setImagenKey(dto.images().mejorUrl());
        }

        // Dentro de la transaccion la entidad esta gestionada,
        // asi que la coleccion LAZY se inicializa sin problema.
        if (dto.genres() != null) {
            for (JikanAnimeDTO.JikanGeneroDTO g : dto.genres()) {
                Genero genero = generoRepository.findByMalId(g.malId())
                        .orElseGet(() -> generoRepository.save(
                                Genero.builder().malId(g.malId()).nombre(g.name()).build()));
                anime.getGeneros().add(genero);
            }
        }

        animeRepository.save(anime);
        return esNuevo;
    }
}