package cl.sugoianime.msauth.config;

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

    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SugoiAnime - API de Identidad y Perfiles")
                        .version("1.0.0")
                        .description("""
                    Microservicio responsable del registro de cuentas, la \
                    autenticacion, la emision de tokens JWT y la gestion de \
                    perfiles de visualizacion.

                    **Requerimientos cubiertos**
                    - RF-08: registro, inicio y cierre de sesion
                    - RF-09: multiples perfiles bajo una cuenta matriz
                    - RNF-05: autenticacion sin estado mediante JWT firmados

                    **Cumplimiento Ley 19.628 sobre proteccion de datos personales**
                    - El registro exige consentimiento explicito y separado por finalidad
                    - Cada consentimiento se persiste con su version, fecha e IP de origen
                    - `DELETE /auth/cuenta` implementa el derecho de eliminacion

                    **Modelo de tokens.** El access token dura 15 minutos y es \
                    sin estado. El refresh token dura 7 dias, se guarda en base \
                    de datos y es revocable: eso es lo que hace posible un cierre \
                    de sesion real, imposible con JWT puro.
                    """)
                        .contact(new Contact()
                                .name("Equipo SugoiAnime")
                                .email("equipo@sugoianime.cl"))
                        .license(new License().name("Uso academico")))

                .servers(List.of(
                        new Server().url("http://localhost:8080/api/auth")
                                .description("Via API Gateway (RNF-04, recomendado)"),
                        new Server().url("http://localhost:8082")
                                .description("Acceso directo (solo depuracion)")
                ))

                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                        Pega aqui el `accessToken` que devuelve `POST /auth/login`. \
                        No incluyas la palabra "Bearer": Swagger la agrega sola.
                        """)));
    }
}
