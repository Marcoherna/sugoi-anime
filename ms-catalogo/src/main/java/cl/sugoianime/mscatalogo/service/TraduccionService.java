package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.config.CacheConfig;
import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.repository.AnimeRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Traduccion de sinopsis al espanol (RF-05).
 *
 * Estrategia de tres capas para controlar el costo de la API externa:
 *   1. Columna sinopsis_es en BD: persistente, sobrevive reinicios.
 *   2. Cache Caffeine en memoria: evita traducir el mismo texto dos veces.
 *   3. Job programado: traduce en lote, NUNCA durante un request del usuario.
 */
@Service
public class TraduccionService {

    private static final Logger log = LoggerFactory.getLogger(TraduccionService.class);
    private static final int LOTE_POR_EJECUCION = 20;

    private final RestClient restClient;
    private final AnimeRepository animeRepository;

    @Value("${traduccion.habilitada}")
    private boolean habilitada;

    @Value("${traduccion.api-url}")
    private String apiUrl;

    @Value("${traduccion.api-key}")
    private String apiKey;

    @Value("${traduccion.idioma-destino}")
    private String idiomaDestino;

    public TraduccionService(@Qualifier("traduccionRestClient") RestClient restClient,
                             AnimeRepository animeRepository) {
        this.restClient = restClient;
        this.animeRepository = animeRepository;
    }

    /**
     * Traduce un texto. El resultado se cachea por el texto de entrada,
     * asi dos animes con la misma sinopsis solo consumen una llamada.
     */
    @Cacheable(value = CacheConfig.CACHE_TRADUCCIONES, unless = "#result == null")
    public String traducir(String texto) {
        if (!habilitada || texto == null || texto.isBlank()) {
            return null;
        }

        try {
            RespuestaTraduccion respuesta = restClient.post()
                    .uri(apiUrl)
                    .body(Map.of(
                            "q",      texto,
                            "source", "en",
                            "target", idiomaDestino,
                            "format", "text",
                            "api_key", apiKey == null ? "" : apiKey
                    ))
                    .retrieve()
                    .body(RespuestaTraduccion.class);

            return respuesta == null ? null : respuesta.translatedText();

        } catch (Exception ex) {
            // Falla suave: sin traduccion se muestra la sinopsis original.
            // Nunca dejamos que un servicio externo tumbe nuestro catalogo.
            log.warn("No se pudo traducir el texto: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Job en lote. Traduce solo lo pendiente y persiste el resultado,
     * de modo que cada sinopsis se traduce una unica vez en la vida.
     */
    @Scheduled(cron = "${traduccion.cron:0 30 3 * * *}")
    @Transactional
    public void traducirPendientes() {
        if (!habilitada) {
            log.debug("Traduccion deshabilitada, se omite el job");
            return;
        }

        List<Anime> pendientes = animeRepository
                .findPendientesDeTraduccion(PageRequest.of(0, LOTE_POR_EJECUCION));

        log.info("Traduciendo {} sinopsis pendientes", pendientes.size());

        int traducidos = 0;
        for (Anime anime : pendientes) {
            String traduccion = traducir(anime.getSinopsis());
            if (traduccion != null) {
                anime.setSinopsisEs(traduccion);
                traducidos++;
            }
        }

        log.info("Traduccion completada: {}/{} exitosas", traducidos, pendientes.size());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaTraduccion(
            @JsonProperty("translatedText") String translatedText
    ) {}
}
