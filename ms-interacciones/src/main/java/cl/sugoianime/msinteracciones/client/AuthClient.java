package cl.sugoianime.msinteracciones.client;

import cl.sugoianime.msinteracciones.client.dto.PerfilExterno;
import cl.sugoianime.msinteracciones.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient client;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AuthClient(@Qualifier("authRestClient") RestClient client,
                      CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.client = client;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * Perfiles de la cuenta autenticada.
     *
     * Se reenvia el token JWT original del usuario: ms-auth resuelve la
     * identidad desde el claim "sub", asi que este servicio nunca necesita
     * credenciales propias ni puede pedir perfiles de otra cuenta.
     *
     * El cache tiene la clave del token para no mezclar usuarios.
     */
    @Cacheable(value = CacheConfig.CACHE_PERFILES, key = "#tokenBearer", unless = "#result.isEmpty()")
    public List<PerfilExterno> listarPerfiles(String tokenBearer) {
        return circuitBreakerFactory.create("auth").run(
                () -> client.get()
                        .uri("/perfiles")
                        .header("Authorization", "Bearer " + tokenBearer)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<PerfilExterno>>() {}),
                throwable -> {
                    log.warn("No se pudieron obtener los perfiles: {}", throwable.toString());
                    return Collections.<PerfilExterno>emptyList();
                });
    }
}
