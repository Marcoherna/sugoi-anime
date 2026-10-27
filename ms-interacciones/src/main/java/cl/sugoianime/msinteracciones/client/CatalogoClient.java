package cl.sugoianime.msinteracciones.client;

import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import cl.sugoianime.msinteracciones.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cliente hacia ms-catalogo.
 *
 * Toda llamada pasa por un circuit breaker: si ms-catalogo cae, este
 * microservicio degrada (devuelve listas vacias) en vez de arrastrar
 * la caida. Es la defensa contra fallos en cascada.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient client;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${recomendaciones.tamano-lote-catalogo}")
    private int tamanoLote;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient client,
                          CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.client = client;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /** Detalle de un anime. Cacheado: el catalogo cambia poco. */
    @Cacheable(value = CacheConfig.CACHE_ANIMES, unless = "#result == null")
    public AnimeResumenExterno obtenerAnime(Long animeId) {
        return circuitBreakerFactory.create("catalogo").run(
                () -> client.get()
                        .uri("/animes/{id}", animeId)
                        .retrieve()
                        .body(AnimeResumenExterno.class),
                throwable -> {
                    log.warn("No se pudo obtener el anime {}: {}", animeId, throwable.toString());
                    return null;
                });
    }

    /** Obtiene varios animes. Devuelve un mapa id -> anime. */
    public Map<Long, AnimeResumenExterno> obtenerAnimes(List<Long> ids) {
        return ids.stream()
                .map(this::obtenerAnime)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(AnimeResumenExterno::id, Function.identity(),
                        (a, b) -> a));
    }

    /**
     * Trae el catalogo completo paginado. Solo lo usa el job nocturno
     * de recomendaciones, nunca un request de usuario.
     */
    public List<AnimeResumenExterno> obtenerCatalogoCompleto() {
        List<AnimeResumenExterno> todos = new ArrayList<>();
        int pagina = 0;
        boolean hayMas = true;

        while (hayMas && pagina < 50) {   // tope de seguridad
            final int p = pagina;

            AnimeResumenExterno.Pagina respuesta = circuitBreakerFactory.create("catalogo").run(
                    () -> client.get()
                            .uri(uri -> uri.path("/animes")
                                    .queryParam("page", p)
                                    .queryParam("size", tamanoLote)
                                    .build())
                            .retrieve()
                            .body(AnimeResumenExterno.Pagina.class),
                    throwable -> {
                        log.error("Fallo al traer la pagina {} del catalogo", p, throwable);
                        return null;
                    });

            if (respuesta == null) {
                break;
            }

            todos.addAll(respuesta.contenidoSeguro());
            hayMas = !respuesta.ultima();
            pagina++;
        }

        log.info("Catalogo obtenido para recomendaciones: {} animes", todos.size());
        return todos;
    }

    public Optional<AnimeResumenExterno> buscar(Long animeId) {
        return Optional.ofNullable(obtenerAnime(animeId));
    }

    public boolean existe(Long animeId) {
        return obtenerAnime(animeId) != null;
    }

    public List<AnimeResumenExterno> vacio() {
        return Collections.emptyList();
    }
}
