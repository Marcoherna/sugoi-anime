package cl.sugoianime.mscatalogo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI catalogoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SugoiAnime - API de Catalogo y Streaming")
                        .version("1.0.0")
                        .description("""
                    Microservicio responsable del catalogo de anime, la entrega de \
                    contenido multimedia via CDN, la sincronizacion con fuentes \
                    externas y la traduccion de sinopsis.

                    **Requerimientos cubiertos**
                    - RF-01: navegacion del catalogo paginado con filtros
                    - RF-02: reproduccion de contenido desde CDN
                    - RF-04: sincronizacion con la API de Jikan (MyAnimeList)
                    - RF-05: traduccion de sinopsis al espanol con cache

                    **Nota legal (Ley 17.336):** todo el contenido multimedia \
                    corresponde a material de dominio publico, licencias Creative \
                    Commons o trailers oficiales. Cada episodio expone su licencia.
                    """)
                        .contact(new Contact()
                                .name("Equipo SugoiAnime")
                                .email("equipo@sugoianime.cl"))
                        .license(new License().name("Uso academico")))

                .servers(List.of(
                        new Server().url("http://localhost:8080/api/catalogo")
                                .description("Via API Gateway (RNF-04, recomendado)"),
                        new Server().url("http://localhost:8081")
                                .description("Acceso directo al microservicio (solo depuracion)")
                ))

                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                        Token emitido por ms-auth y validado en el API Gateway (RNF-05).
                        En este microservicio los endpoints de consulta son publicos.
                        """)));
    }
}
