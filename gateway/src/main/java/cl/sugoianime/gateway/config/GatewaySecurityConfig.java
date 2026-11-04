package cl.sugoianime.gateway.config;

import cl.sugoianime.gateway.security.RespuestasJsonAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad perimetral (RNF-04, RNF-05).
 *
 * Aqui se decide, para TODO el sistema, que es publico y que exige
 * autenticacion. Los microservicios detras no necesitan conocer esta
 * politica: solo verifican que el token sea valido.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /** Rutas accesibles sin token. */
    private static final String[] PUBLICAS = {
            // Autenticacion: registro, login y renovacion no pueden exigir token
            "/api/auth/auth/registro",
            "/api/auth/auth/login",
            "/api/auth/auth/refresh",
            "/api/auth/auth/logout",

            // Documentacion
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/api/*/v3/api-docs/**",

            // Salud y fallbacks
            "/actuator/health/**",
            "/actuator/info",
            "/fallback/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            RespuestasJsonAuth respuestas) {

        return http
                // API sin estado detras de un cliente SPA: no hay cookies de
                // sesion, por lo que el vector CSRF no aplica.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .authorizeExchange(ex -> ex
                        // El preflight CORS jamas lleva Authorization
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .pathMatchers(PUBLICAS).permitAll()

                        // Catalogo: la navegacion es publica, como en cualquier
                        // plataforma de streaming. Se ve el catalogo sin cuenta;
                        // se necesita cuenta para reproducir e interactuar.
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/animes/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/episodios/**").permitAll()

                        // Administracion del catalogo y cuentas: solo rol ADMIN
                        .pathMatchers("/api/catalogo/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/auth/admin/**").hasRole("ADMIN")

                        // Todo lo demas exige token valido
                        .anyExchange().authenticated())

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(conversorReactivo()))
                        // Respuestas JSON en vez de cuerpos vacios
                        .authenticationEntryPoint(respuestas.entryPoint401())
                        .accessDeniedHandler(respuestas.accessDenied403()))

                .build();
    }

    /**
     * El claim "roles" del JWT se traduce a authorities ROLE_*.
     * Spring busca por defecto en "scope"/"scp"; hay que indicarselo.
     *
     * En WebFlux el converter debe ser reactivo, de ahi el adapter.
     */
    private ReactiveJwtAuthenticationConverterAdapter conversorReactivo() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("roles");
        granted.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(granted);

        return new ReactiveJwtAuthenticationConverterAdapter(conversor);
    }
}
