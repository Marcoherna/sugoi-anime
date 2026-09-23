package cl.sugoianime.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Identificador de correlacion para trazabilidad distribuida.
 *
 * PROBLEMA QUE RESUELVE: una peticion del usuario puede tocar el gateway,
 * ms-interacciones, ms-catalogo y ms-auth. Sin un identificador comun,
 * reconstruir que paso implica comparar timestamps entre cuatro logs
 * distintos. Con X-Request-Id basta un grep.
 *
 * Tambien registra la latencia de cada peticion, util para documentar el
 * impacto del CDN en las pruebas de carga del Sprint 8 (RNF-07).
 */
@Component
public class CorrelacionGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelacionGlobalFilter.class);
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long inicio = System.currentTimeMillis();

        // Respeta un id existente (por si hay un proxy delante), o crea uno
        String requestId = exchange.getRequest()
                .getHeaders().getFirst(HEADER_REQUEST_ID);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        final String idFinal = requestId;

        // Se propaga hacia el microservicio destino
        ServerHttpRequest peticion = exchange.getRequest().mutate()
                .header(HEADER_REQUEST_ID, idFinal)
                .build();

        // Y tambien de vuelta al cliente, para que pueda reportarlo
        exchange.getResponse().getHeaders().add(HEADER_REQUEST_ID, idFinal);

        String metodo = peticion.getMethod().name();
        String ruta = peticion.getPath().value();

        return chain.filter(exchange.mutate().request(peticion).build())
                .doOnSuccess(v -> {
                    long duracion = System.currentTimeMillis() - inicio;
                    Integer estado = exchange.getResponse().getStatusCode() == null
                            ? null : exchange.getResponse().getStatusCode().value();

                    log.info("[{}] {} {} -> {} ({} ms)",
                            idFinal, metodo, ruta, estado, duracion);
                })
                .doOnError(error ->
                        log.error("[{}] {} {} fallo: {}",
                                idFinal, metodo, ruta, error.toString()));
    }

    @Override
    public int getOrder() {
        // Antes que todo lo demas, para que el id exista en toda la cadena
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
