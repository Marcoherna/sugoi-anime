package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.EstadoAnime;
import cl.sugoianime.mscatalogo.domain.Genero;
import cl.sugoianime.mscatalogo.dto.jikan.JikanAnimeDTO;
import cl.sugoianime.mscatalogo.dto.jikan.JikanPaginaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sincronizacion del catalogo con la API de Jikan / MyAnimeList (RF-04).
 *
 * MITIGACION DEL RIESGO R02 (rate limit de Jikan), tres controles:
 *   1. Pausa deliberada entre llamadas (Jikan permite ~3 req/seg).
 *   2. Circuit breaker: si Jikan empieza a fallar, dejamos de insistir.
 *   3. Dataset semilla en V2__seed_catalogo.sql: el catalogo nunca queda
 *      vacio aunque la API externa este caida durante una demo.
 */
@Service
public class JikanSyncService {

    private static final Logger log = LoggerFactory.getLogger(JikanSyncService.class);

    private final RestClient jikanClient;
    private final AnimePersistenceService persistenceService;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;



    @Value("${jikan.delay-entre-llamadas-ms}")
    private long delayMs;

    @Value("${jikan.paginas-por-sincronizacion}")
    private int paginasPorSync;

    @Value("${jikan.sincronizacion-habilitada}")
    private boolean habilitada;

    public JikanSyncService(@Qualifier("jikanRestClient") RestClient jikanClient,
                            AnimePersistenceService persistenceService,
                            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.jikanClient = jikanClient;
        this.persistenceService = persistenceService;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /** Job diario a las 03:00. Desactivado por defecto en desarrollo. */
    @Scheduled(cron = "${jikan.cron:0 0 3 * * *}")
    public void sincronizacionProgramada() {
        if (!habilitada) {
            log.debug("Sincronizacion Jikan deshabilitada, se omite");
            return;
        }
        sincronizar(paginasPorSync);
    }

    /** Sincronizacion manual, invocable desde el endpoint de administracion. */
    public ResultadoSync sincronizar(int paginas) {
        log.info("Iniciando sincronizacion con Jikan: {} paginas", paginas);

        int nuevos = 0;
        int actualizados = 0;
        int errores = 0;

        for (int pagina = 1; pagina <= paginas; pagina++) {
            final int p = pagina;

            JikanPaginaResponse respuesta = circuitBreakerFactory.create("jikan")
                    .run(() -> consultarPagina(p),
                            throwable -> {
                                // Pasamos el throwable completo como ultimo argumento:
                                // SLF4J imprime el stack trace en vez de solo el mensaje.
                                log.error("Fallo al consultar la pagina {} de Jikan", p, throwable);
                                return null;
                            });

            if (respuesta == null) {
                errores++;
                continue;
            }

            for (JikanAnimeDTO dto : respuesta.datosSeguro()) {
                if (!dto.esValido()) {
                    continue;
                }
                if (persistenceService.guardarOActualizar(dto)) {
                    nuevos++;
                } else {
                    actualizados++;
                }
            }

            if (!respuesta.hayMasPaginas()) {
                break;
            }
            esperar();
        }

        ResultadoSync resultado = new ResultadoSync(nuevos, actualizados, errores);
        log.info("Sincronizacion finalizada: {}", resultado);
        return resultado;
    }

    private JikanPaginaResponse consultarPagina(int pagina) {
        return jikanClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/top/anime")
                        .queryParam("page", pagina)
                        .queryParam("limit", 25)
                        .build())
                .retrieve()
                .body(JikanPaginaResponse.class);
    }



    /** Pausa deliberada para respetar el rate limit de Jikan (riesgo R02). */
    private void esperar() {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sincronizacion interrumpida");
        }
    }

    public record ResultadoSync(int nuevos, int actualizados, int errores) {}
}
