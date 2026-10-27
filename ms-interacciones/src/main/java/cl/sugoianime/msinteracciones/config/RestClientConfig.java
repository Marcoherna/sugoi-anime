package cl.sugoianime.msinteracciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP hacia los otros microservicios.
 *
 * Los timeouts se configuran en spring.http.client del application.yml,
 * no aqui: son un parametro operacional que debe poder ajustarse por
 * entorno sin recompilar.
 */
@Configuration
public class RestClientConfig {

    @Bean("catalogoRestClient")
    public RestClient catalogoRestClient(RestClient.Builder builder,
                                         ServiciosProperties propiedades) {
        return builder
                .baseUrl(propiedades.catalogoUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean("authRestClient")
    public RestClient authRestClient(RestClient.Builder builder,
                                     ServiciosProperties propiedades) {
        return builder
                .baseUrl(propiedades.authUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
