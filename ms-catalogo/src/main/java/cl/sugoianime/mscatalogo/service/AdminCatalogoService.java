package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.Episodio;
import cl.sugoianime.mscatalogo.domain.EstadoAnime;
import cl.sugoianime.mscatalogo.domain.Genero;
import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.CrearAnimeRequest;
import cl.sugoianime.mscatalogo.dto.CrearEpisodioRequest;
import cl.sugoianime.mscatalogo.dto.EpisodioDTO;
import cl.sugoianime.mscatalogo.dto.jikan.JikanAnimeDTO;
import cl.sugoianime.mscatalogo.dto.jikan.JikanPaginaResponse;
import cl.sugoianime.mscatalogo.exception.RecursoNoEncontradoException;
import cl.sugoianime.mscatalogo.mapper.AnimeMapper;
import cl.sugoianime.mscatalogo.repository.AnimeRepository;
import cl.sugoianime.mscatalogo.repository.EpisodioRepository;
import cl.sugoianime.mscatalogo.repository.GeneroRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdminCatalogoService {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogoService.class);

    private final AnimeRepository animeRepository;
    private final EpisodioRepository episodioRepository;
    private final GeneroRepository generoRepository;
    private final AnimeMapper animeMapper;
    private final RestClient jikanClient;
    private final AnimePersistenceService persistenceService;
    private final TraduccionService traduccionService;

    public AdminCatalogoService(AnimeRepository animeRepository,
                                EpisodioRepository episodioRepository,
                                GeneroRepository generoRepository,
                                AnimeMapper animeMapper,
                                @Qualifier("jikanRestClient") RestClient jikanClient,
                                AnimePersistenceService persistenceService,
                                TraduccionService traduccionService) {
        this.animeRepository = animeRepository;
        this.episodioRepository = episodioRepository;
        this.generoRepository = generoRepository;
        this.animeMapper = animeMapper;
        this.jikanClient = jikanClient;
        this.persistenceService = persistenceService;
        this.traduccionService = traduccionService;
    }

    /**
     * Busca candidatos de anime por nombre usando AniList GraphQL (con respaldo Jikan),
     * garantizando respuesta sub-segundo (< 1s) sin bloqueos de red.
     */
    public List<JikanAnimeDTO> buscarEnJikanPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nombre de la serie a buscar.");
        }
        List<JikanAnimeDTO> desdeAniList = consultarAniListPorNombre(nombre.trim());
        if (!desdeAniList.isEmpty()) {
            return desdeAniList;
        }
        JikanPaginaResponse respuesta = jikanClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime")
                        .queryParam("q", nombre.trim())
                        .queryParam("limit", 6)
                        .queryParam("sfw", true)
                        .build())
                .retrieve()
                .body(JikanPaginaResponse.class);

        if (respuesta == null) {
            return List.of();
        }
        return respuesta.datosSeguro().stream()
                .filter(JikanAnimeDTO::esValido)
                .toList();
    }

    /**
     * Importa una serie buscando por su nombre (toma la mejor coincidencia)
     * o por su malId especifico, persiste sus generos/portada/metadatos y traduce su sinopsis.
     */
    @Transactional
    public AnimeDetalleDTO importarDesdeJikan(String nombre, Integer malId) {
        JikanAnimeDTO dto = null;

        if (malId != null) {
            dto = consultarAniListPorMalId(malId);
            if (dto == null) {
                JikanSingleResponse individual = jikanClient.get()
                        .uri("/anime/{id}", malId)
                        .retrieve()
                        .body(JikanSingleResponse.class);
                if (individual != null && individual.data() != null && individual.data().esValido()) {
                    dto = individual.data();
                }
            }
            if (dto == null) {
                throw new IllegalArgumentException(
                        "No se encontró la serie con ID=" + malId + " en la API.");
            }
        } else {
            List<JikanAnimeDTO> resultados = buscarEnJikanPorNombre(nombre);
            if (resultados.isEmpty()) {
                throw new IllegalArgumentException(
                        "No se encontró ninguna serie en la API con el nombre: \"" + nombre + "\"");
            }
            dto = resultados.getFirst();
        }

        Anime anime = persistenceService.guardarOActualizarEntidad(dto);

        if ((anime.getSinopsisEs() == null || anime.getSinopsisEs().isBlank())
                && anime.getSinopsis() != null && !anime.getSinopsis().isBlank()) {
            String traduccion = traduccionService.traducir(anime.getSinopsis());
            if (traduccion != null && !traduccion.isBlank()) {
                anime.setSinopsisEs(traduccion);
                anime = animeRepository.save(anime);
            }
        }

        log.info("Serie importada desde API: id={}, malId={}, titulo={}",
                anime.getId(), anime.getMalId(), anime.getTitulo());

        return animeMapper.aDetalleDTO(anime);
    }

    private List<JikanAnimeDTO> consultarAniListPorNombre(String nombre) {
        try {
            String query = """
                    query ($search: String) {
                      Page(page: 1, perPage: 6) {
                        media(search: $search, type: ANIME, isAdult: false, sort: POPULARITY_DESC) {
                          id
                          idMal
                          title { romaji english native }
                          description(asHtml: false)
                          episodes
                          averageScore
                          status
                          startDate { year }
                          coverImage { extraLarge large }
                          genres
                        }
                      }
                    }
                    """;
            java.util.Map<String, Object> body = java.util.Map.of(
                    "query", query,
                    "variables", java.util.Map.of("search", nombre)
            );
            AniListRootResponse resp = RestClient.create().post()
                    .uri("https://graphql.anilist.co")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(AniListRootResponse.class);

            if (resp == null || resp.data() == null || resp.data().page() == null
                    || resp.data().page().media() == null) {
                return List.of();
            }
            return resp.data().page().media().stream()
                    .map(this::convertirMediaAJikanDTO)
                    .filter(JikanAnimeDTO::esValido)
                    .toList();
        } catch (Exception ex) {
            log.warn("Fallo consulta rapida a AniList para '{}': {}", nombre, ex.getMessage());
            return List.of();
        }
    }

    private JikanAnimeDTO consultarAniListPorMalId(Integer malId) {
        try {
            String query = """
                    query ($idMal: Int) {
                      Media(idMal: $idMal, type: ANIME) {
                        id
                        idMal
                        title { romaji english native }
                        description(asHtml: false)
                        episodes
                        averageScore
                        status
                        startDate { year }
                        coverImage { extraLarge large }
                        genres
                      }
                    }
                    """;
            java.util.Map<String, Object> body = java.util.Map.of(
                    "query", query,
                    "variables", java.util.Map.of("idMal", malId)
            );
            AniListSingleRootResponse resp = RestClient.create().post()
                    .uri("https://graphql.anilist.co")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(AniListSingleRootResponse.class);

            if (resp == null || resp.data() == null || resp.data().media() == null) {
                return null;
            }
            return convertirMediaAJikanDTO(resp.data().media());
        } catch (Exception ex) {
            log.warn("Fallo consulta a AniList por malId={}: {}", malId, ex.getMessage());
            return null;
        }
    }

    private JikanAnimeDTO convertirMediaAJikanDTO(AniListMedia m) {
        Integer malId = m.idMal() != null ? m.idMal() : m.id();
        String titulo = m.title() != null
                ? (m.title().romaji() != null ? m.title().romaji() : m.title().english())
                : "Sin titulo";
        String tituloJp = m.title() != null ? m.title().nativeTitle() : null;
        String sinopsisLimpia = m.description() != null
                ? m.description().replaceAll("<[^>]+>", "").replaceAll("\\(Source:.*\\)", "").trim()
                : "Sin sinopsis disponible.";
        java.math.BigDecimal score = m.averageScore() != null
                ? java.math.BigDecimal.valueOf(m.averageScore()).divide(java.math.BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.valueOf(8.0);
        String estadoJikan = switch (m.status() == null ? "" : m.status()) {
            case "RELEASING" -> "Currently Airing";
            case "NOT_YET_RELEASED" -> "Not yet aired";
            default -> "Finished Airing";
        };
        String urlImagen = m.coverImage() != null
                ? (m.coverImage().extraLarge() != null ? m.coverImage().extraLarge() : m.coverImage().large())
                : null;
        cl.sugoianime.mscatalogo.dto.jikan.JikanImagenDTO imagenes = new cl.sugoianime.mscatalogo.dto.jikan.JikanImagenDTO(
                new cl.sugoianime.mscatalogo.dto.jikan.JikanImagenDTO.Formato(urlImagen, urlImagen)
        );
        Integer anio = (m.startDate() != null && m.startDate().year() != null)
                ? m.startDate().year()
                : 2024;
        JikanAnimeDTO.Aired aired = new JikanAnimeDTO.Aired(
                new JikanAnimeDTO.Aired.Prop(new JikanAnimeDTO.Aired.Prop.From(anio))
        );
        List<JikanAnimeDTO.JikanGeneroDTO> generos = (m.genres() == null ? List.<String>of() : m.genres())
                .stream()
                .map(g -> new JikanAnimeDTO.JikanGeneroDTO(null, g))
                .toList();

        return new JikanAnimeDTO(
                malId,
                titulo,
                tituloJp,
                sinopsisLimpia,
                m.episodes() != null ? m.episodes() : 12,
                score,
                estadoJikan,
                imagenes,
                aired,
                generos
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JikanSingleResponse(@JsonProperty("data") JikanAnimeDTO data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListRootResponse(@JsonProperty("data") AniListPageData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListSingleRootResponse(@JsonProperty("data") AniListSingleData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListPageData(@JsonProperty("Page") AniListPage page) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListSingleData(@JsonProperty("Media") AniListMedia media) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListPage(@JsonProperty("media") List<AniListMedia> media) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListMedia(
            @JsonProperty("id") Integer id,
            @JsonProperty("idMal") Integer idMal,
            @JsonProperty("title") AniListTitle title,
            @JsonProperty("description") String description,
            @JsonProperty("episodes") Integer episodes,
            @JsonProperty("duration") Integer duration,
            @JsonProperty("averageScore") Integer averageScore,
            @JsonProperty("status") String status,
            @JsonProperty("startDate") AniListDate startDate,
            @JsonProperty("coverImage") AniListCover coverImage,
            @JsonProperty("genres") List<String> genres,
            @JsonProperty("streamingEpisodes") List<AniListStreamingEp> streamingEpisodes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListTitle(
            @JsonProperty("romaji") String romaji,
            @JsonProperty("english") String english,
            @JsonProperty("native") String nativeTitle
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListDate(@JsonProperty("year") Integer year) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListCover(
            @JsonProperty("extraLarge") String extraLarge,
            @JsonProperty("large") String large
    ) {}

    @Transactional
    public AnimeDetalleDTO crearAnime(CrearAnimeRequest request) {
        Integer malId = request.malId();
        if (malId == null) {
            do {
                malId = ThreadLocalRandom.current().nextInt(900_000, 9_999_999);
            } while (animeRepository.existsByMalId(malId));
        } else if (animeRepository.existsByMalId(malId)) {
            throw new IllegalArgumentException(
                    "Ya existe una serie registrada con el malId " + malId);
        }

        EstadoAnime estado = parsearEstado(request.estado());

        Anime anime = Anime.builder()
                .malId(malId)
                .titulo(request.titulo().trim())
                .tituloJapones(request.tituloJapones() != null ? request.tituloJapones().trim() : null)
                .sinopsis(request.sinopsis())
                .sinopsisEs(request.sinopsis())
                .anio(request.anio())
                .episodios(request.episodios())
                .puntuacion(request.puntuacion())
                .imagenKey(request.imagenKey() != null && !request.imagenKey().isBlank()
                        ? request.imagenKey().trim() : null)
                .estado(estado)
                .generos(resolverGeneros(request))
                .build();

        anime = animeRepository.save(anime);
        log.info("Serie creada por administrador: id={}, titulo={}", anime.getId(), anime.getTitulo());

        return animeMapper.aDetalleDTO(anime);
    }

    @Transactional
    public AnimeDetalleDTO actualizarAnime(Long id, CrearAnimeRequest request) {
        Anime anime = animeRepository.findWithDetalleById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.anime(id));

        anime.setTitulo(request.titulo().trim());
        anime.setTituloJapones(request.tituloJapones() != null ? request.tituloJapones().trim() : null);
        if (request.sinopsis() != null) {
            anime.setSinopsis(request.sinopsis());
            anime.setSinopsisEs(request.sinopsis());
        }
        anime.setAnio(request.anio());
        anime.setEpisodios(request.episodios());
        anime.setPuntuacion(request.puntuacion());
        if (request.imagenKey() != null) {
            anime.setImagenKey(request.imagenKey().isBlank() ? null : request.imagenKey().trim());
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            anime.setEstado(parsearEstado(request.estado()));
        }
        if (request.generoIds() != null) {
            anime.setGeneros(resolverGeneros(request));
        }

        anime = animeRepository.save(anime);
        log.info("Serie actualizada por administrador: id={}, titulo={}", anime.getId(), anime.getTitulo());

        return animeMapper.aDetalleDTO(anime);
    }

    @Transactional
    public void eliminarAnime(Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.anime(id));
        animeRepository.delete(anime);
        log.info("Serie eliminada por administrador: id={}", id);
    }

    @Transactional
    public EpisodioDTO guardarEpisodio(CrearEpisodioRequest request) {
        Anime anime = animeRepository.findById(request.animeId())
                .orElseThrow(() -> RecursoNoEncontradoException.anime(request.animeId()));

        Episodio episodio = episodioRepository
                .findByAnimeIdAndNumero(anime.getId(), request.numero())
                .orElseGet(() -> Episodio.builder()
                        .anime(anime)
                        .numero(request.numero())
                        .build());

        episodio.setTitulo(request.titulo().trim());
        episodio.setDuracionSeg(request.duracionSeg());
        episodio.setVideoKey(request.videoKey().trim());
        episodio.setLicencia(request.licencia().trim());
        episodio.setFuenteUrl(request.fuenteUrl() != null && !request.fuenteUrl().isBlank()
                ? request.fuenteUrl().trim() : null);

        episodio = episodioRepository.save(episodio);
        log.info("Episodio guardado: animeId={}, numero={}, videoKey={}",
                anime.getId(), episodio.getNumero(), episodio.getVideoKey());

        return animeMapper.aEpisodioDTO(episodio);
    }

    @Transactional
    public void eliminarEpisodio(Long episodioId) {
        Episodio episodio = episodioRepository.findById(episodioId)
                .orElseThrow(() -> RecursoNoEncontradoException.episodio(episodioId));
        episodioRepository.delete(episodio);
        log.info("Episodio eliminado por administrador: id={}", episodioId);
    }

    /**
     * Autocompleta los metadatos de un capitulo (titulo traducido al espanol, duracion en segundos,
     * licencia y fuenteUrl) a partir del animeId y el numero de capitulo elegido.
     */
    @Transactional(readOnly = true)
    public SugerenciaEpisodioDTO sugerirDatosEpisodio(Long animeId, Integer numero) {
        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> RecursoNoEncontradoException.anime(animeId));

        int num = (numero != null && numero > 0) ? numero : 1;

        // 1. Si el capitulo ya existe en nuestra base de datos, devolvemos sus datos actuales
        java.util.Optional<Episodio> existente = episodioRepository.findByAnimeIdAndNumero(animeId, num);
        if (existente.isPresent()) {
            Episodio ep = existente.get();
            return new SugerenciaEpisodioDTO(
                    ep.getNumero(),
                    ep.getTitulo(),
                    ep.getDuracionSeg(),
                    ep.getLicencia(),
                    ep.getFuenteUrl() != null ? ep.getFuenteUrl() : "https://myanimelist.net/anime/" + anime.getMalId(),
                    ep.getVideoKey(),
                    anime.getEpisodios() != null ? anime.getEpisodios() : 24
            );
        }

        // 2. Consultar AniList (streamingEpisodes + duration)
        String tituloIngles = null;
        int duracionMin = 24;
        String fuenteUrl = anime.getMalId() != null
                ? "https://myanimelist.net/anime/" + anime.getMalId()
                : "https://sugoianime.cl/licencias";

        try {
            String query = """
                    query ($idMal: Int) {
                      Media(idMal: $idMal, type: ANIME) {
                        duration
                        streamingEpisodes { title url }
                      }
                    }
                    """;
            java.util.Map<String, Object> body = java.util.Map.of(
                    "query", query,
                    "variables", java.util.Map.of("idMal", anime.getMalId() != null ? anime.getMalId() : 0)
            );
            AniListSingleRootResponse resp = RestClient.create().post()
                    .uri("https://graphql.anilist.co")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(AniListSingleRootResponse.class);

            if (resp != null && resp.data() != null && resp.data().media() != null) {
                AniListMedia m = resp.data().media();
                if (m.duration() != null && m.duration() > 0) {
                    duracionMin = m.duration();
                }
                if (m.streamingEpisodes() != null) {
                    java.util.regex.Pattern patron = java.util.regex.Pattern.compile(
                            "(?i)^episode\\s+0*" + num + "\\b\\s*[-:]?\\s*(.+)$");
                    for (AniListStreamingEp sep : m.streamingEpisodes()) {
                        if (sep.title() != null) {
                            java.util.regex.Matcher matcher = patron.matcher(sep.title().trim());
                            if (matcher.matches()) {
                                tituloIngles = matcher.group(1).trim();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("No se pudo obtener streamingEpisodes desde AniList: {}", ex.getMessage());
        }

        // 3. Respaldo en Kitsu API por episodio exacto si AniList no tenia el titulo del capitulo
        if (tituloIngles == null && anime.getMalId() != null) {
            try {
                KitsuMappingResponse mapping = RestClient.create().get()
                        .uri("https://kitsu.io/api/edge/mappings?filter[externalSite]=myanimelist/anime&filter[externalId]={id}&include=item",
                                anime.getMalId())
                        .header("Accept", "application/vnd.api+json")
                        .retrieve()
                        .body(KitsuMappingResponse.class);

                if (mapping != null && mapping.included() != null && !mapping.included().isEmpty()) {
                    String kitsuAnimeId = mapping.included().getFirst().id();
                    KitsuEpisodesResponse eps = RestClient.create().get()
                            .uri("https://kitsu.io/api/edge/episodes?filter[mediaType]=Anime&filter[media_id]={id}&filter[number]={num}",
                                    kitsuAnimeId, num)
                            .header("Accept", "application/vnd.api+json")
                            .retrieve()
                            .body(KitsuEpisodesResponse.class);
                    if (eps != null && eps.data() != null && !eps.data().isEmpty()
                            && eps.data().getFirst().attributes() != null) {
                        KitsuEpisodeAttr attr = eps.data().getFirst().attributes();
                        if (attr.canonicalTitle() != null && !attr.canonicalTitle().isBlank()) {
                            tituloIngles = attr.canonicalTitle().trim();
                        }
                        if (attr.length() != null && attr.length() > 0) {
                            duracionMin = attr.length();
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("No se pudo obtener episodio desde Kitsu: {}", ex.getMessage());
            }
        }

        String tituloFinal;
        if (tituloIngles != null && !tituloIngles.isBlank()) {
            String traducido = traduccionService.traducir(tituloIngles);
            tituloFinal = (traducido != null && !traducido.isBlank()) ? traducido : tituloIngles;
        } else {
            tituloFinal = "Episodio " + num + " - " + anime.getTitulo();
        }

        return new SugerenciaEpisodioDTO(
                num,
                tituloFinal,
                duracionMin * 60,
                "CC-BY-3.0",
                fuenteUrl,
                null,
                anime.getEpisodios() != null ? anime.getEpisodios() : 24
        );
    }

    public record SugerenciaEpisodioDTO(
            Integer numero,
            String titulo,
            Integer duracionSeg,
            String licencia,
            String fuenteUrl,
            String videoKey,
            Integer totalEpisodiosSerie
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListStreamingEp(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KitsuMappingResponse(@JsonProperty("included") List<KitsuIncludedItem> included) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KitsuIncludedItem(@JsonProperty("id") String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KitsuEpisodesResponse(@JsonProperty("data") List<KitsuEpisodeData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KitsuEpisodeData(@JsonProperty("attributes") KitsuEpisodeAttr attributes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KitsuEpisodeAttr(
            @JsonProperty("canonicalTitle") String canonicalTitle,
            @JsonProperty("length") Integer length
    ) {}

    private Set<Genero> resolverGeneros(CrearAnimeRequest request) {
        Set<Genero> generos = new LinkedHashSet<>();
        if (request.generoIds() != null && !request.generoIds().isEmpty()) {
            generos.addAll(generoRepository.findAllById(request.generoIds()));
        }
        return generos;
    }

    private EstadoAnime parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return EstadoAnime.FINALIZADO;
        }
        try {
            return EstadoAnime.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EstadoAnime.DESCONOCIDO;
        }
    }
}

