package cl.sugoianime.msauth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Emision y validacion de JWT con HMAC-SHA256 (RNF-05).
 *
 * El mismo secreto lo usa el API Gateway para validar los tokens.
 * Si cambia en un lado y no en el otro, todos los tokens dejan de servir.
 */
@Configuration
public class JwtConfig {

    private final SecretKey clave;

    public JwtConfig(JwtProperties propiedades) {
        byte[] bytes = Base64.getDecoder().decode(propiedades.secret());

        // HS256 exige al menos 256 bits (32 bytes). Fallar aqui, al
        // arrancar, es infinitamente mejor que emitir tokens debiles.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes tras decodificar Base64. "
                            + "Genera uno con: openssl rand -base64 64 | tr -d '\\n'");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
