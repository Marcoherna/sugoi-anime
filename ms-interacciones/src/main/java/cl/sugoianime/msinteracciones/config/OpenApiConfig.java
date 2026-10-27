package cl.sugoianime.msinteracciones.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI interaccionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SugoiAnime - API de Interacciones y Recomendaciones")
                        .version("1.0.0")
                        .description("""
                    Microservicio responsable de lo que el usuario HACE con el \
                    contenido: marcarlo como favorito, avanzar en su reproduccion \
                    y recibir sugerencias personalizadas.

                    **Requerimientos cubiertos**
                    - RF-03: registro del punto exacto de reproduccion
                    - RF-06: favoritos por perfil
                    - RF-07: recomendaciones por filtrado basado en contenido

                    **Nota de arquitectura (RNF-02).** Este servicio almacena \
                    `perfilId` y `animeId` como referencias logicas, sin claves \
                    foraneas: esas entidades viven en las bases de datos de \
                    ms-auth y ms-catalogo respectivamente. La integridad se \
                    valida por HTTP, no por el motor de base de datos.

                    **Algoritmo de recomendacion.** Filtrado basado en contenido \
                    mediante similitud de coseno sobre vectores binarios de \
                    genero. Se calcula en un job nocturno y se persiste, nunca \
                    en tiempo de request.

                    **Ley 19.628.** El historial se usa para recomendaciones solo \
                    si el usuario otorgo ese consentimiento especifico durante \
                    el registro.
                    """)
                        .contact(new Contact()
                                .name("Equipo SugoiAnime")
                                .email("equipo@sugoianime.cl"))
                        .license(new License().name("Uso academico")))

                .servers(List.of(
                        new Server().url("http://localhost:8080/api/interacciones")
                                .description("Via API Gateway (RNF-04, recomendado)"),
                        new Server().url("http://localhost:8083")
                                .description("Acceso directo (solo depuracion)")
                ))

                // Todos los endpoints exigen token: aqui no hay datos publicos.
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA, new SecurityScheme()
                                .name(ESQUEMA)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token emitido por ms-auth")));
    }
}
