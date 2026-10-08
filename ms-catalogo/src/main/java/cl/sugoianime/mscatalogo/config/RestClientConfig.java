package cl.sugoianime.mscatalogo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${jikan.base-url}")
    private String jikanBaseUrl;

    /**
     * Cliente para la API de Jikan (RF-04).
     *
     * Los timeouts NO se configuran aqui: vienen del bloque
     * spring.http.client del application.yml y Boot los aplica
     * al RestClient.Builder autoconfigurado que recibimos.
     */
    @Bean("jikanRestClient")
    public RestClient jikanRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(jikanBaseUrl)
                .defaultHeader("User-Agent", "SugoiAnime/1.0 (proyecto academico)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /** Cliente generico para la API de traduccion (RF-05). */
    @Bean("traduccionRestClient")
    public RestClient traduccionRestClient(RestClient.Builder builder) {
        return builder
                .defaultHeader("Accept", "application/json")
                .build();
    }
}