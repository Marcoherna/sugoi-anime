package cl.sugoianime.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Portada del Swagger unificado.
 *
 * El gateway no expone API propia: agrega las especificaciones de los
 * tres microservicios en un selector desplegable. Un evaluador entra a
 * una sola URL y explora todo el sistema.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SugoiAnime - API Gateway")
                        .version("1.0.0")
                        .description("""
                    Punto unico de entrada al sistema (RNF-04).

                    Selecciona una API en el desplegable superior derecho \
                    para explorar cada microservicio.

                    **Rutas**
                    | Prefijo | Microservicio | Puerto interno |
                    |---|---|---|
                    | `/api/catalogo/**` | Catalogo y Streaming | 8081 |
                    | `/api/auth/**` | Identidad y Perfiles | 8082 |
                    | `/api/perfiles/**` | Identidad y Perfiles | 8082 |
                    | `/api/interacciones/**` | Interacciones | 8083 |

                    **Seguridad (RNF-05).** El gateway valida la firma, el \
                    emisor y la vigencia de cada JWT antes de reenviar la \
                    peticion. Los microservicios vuelven a validarlo: es \
                    defensa en profundidad, no redundancia.

                    **Acceso publico.** La navegacion del catalogo y el \
                    registro/login no requieren token. Todo lo demas si.

                    **Resiliencia.** Cada ruta esta protegida por un circuit \
                    breaker con respuesta de respaldo: si un microservicio \
                    cae, el resto del sistema sigue operativo.

                    **Ley 21.459.** Los microservicios y las bases de datos \
                    no exponen puertos al exterior. Este gateway es la unica \
                    superficie accesible del sistema.
                    """)
                        .contact(new Contact()
                                .name("Equipo SugoiAnime")
                                .email("equipo@sugoianime.cl"))
                        .license(new License().name("Uso academico")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")));
    }
}
