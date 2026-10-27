package cl.sugoianime.msinteracciones.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**"
    };

    private final SecretKey clave;

    public SecurityConfig(@Value("${jwt.secret}") String secreto) {
        this.clave = new SecretKeySpec(decodificar(secreto), "HmacSHA256");
    }

    /**
     * IMPORTANTE: esta logica debe ser IDENTICA a la de ms-auth y la del
     * Gateway. Si un servicio decodifica el secreto como Base64 y otro
     * como UTF-8, las claves resultantes difieren y todos los tokens se
     * rechazan con un 401 generico que no apunta a la causa.
     */
    private static byte[] decodificar(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no esta definido");
        }
        String limpio = secreto.trim();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(limpio);
        } catch (IllegalArgumentException e1) {
            try {
                bytes = Base64.getUrlDecoder().decode(limpio);
            } catch (IllegalArgumentException e2) {
                bytes = limpio.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes (tiene " + bytes.length + ")");
        }
        return bytes;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        // Todo lo demas exige token: aqui no hay datos publicos.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder())
                                .jwtAuthenticationConverter(conversorAuthorities())))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private JwtAuthenticationConverter conversorAuthorities() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("roles");
        granted.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(granted);
        return conversor;
    }

    /** Import necesario para que el compilador no marque ImmutableSecret sin uso. */
    @SuppressWarnings("unused")
    private static Class<?> refImmutableSecret() {
        return ImmutableSecret.class;
    }
}
