package cl.sugoianime.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Decodificacion y validacion de los JWT emitidos por ms-auth (RNF-05).
 *
 * CRITICO: la logica de decodificacion del secreto debe ser IDENTICA a
 * la de ms-auth y ms-interacciones. Si un servicio interpreta el secreto
 * como Base64 y otro como UTF-8, las claves resultantes son distintas y
 * todos los tokens se rechazan con un 401 generico que no apunta a la
 * causa real. Es uno de los errores mas frustrantes de diagnosticar en
 * esta arquitectura.
 */
@Configuration
public class JwtDecoderConfig {

    private final SecretKey clave;
    private final String emisorEsperado;

    public JwtDecoderConfig(@Value("${jwt.secret}") String secreto,
                            @Value("${jwt.emisor-esperado}") String emisorEsperado) {
        this.clave = new SecretKeySpec(decodificar(secreto), "HmacSHA256");
        this.emisorEsperado = emisorEsperado;
    }

    private static byte[] decodificar(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no esta definido en el gateway");
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

    /**
     * Decodificador REACTIVO. El gateway corre sobre WebFlux, asi que
     * necesita ReactiveJwtDecoder, no el JwtDecoder imperativo que usan
     * ms-auth y ms-interacciones.
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Ademas de la firma, se valida que el emisor sea el esperado y
        // que el token no haya expirado. Firma valida no basta: un token
        // emitido por otro sistema con el mismo secreto seria aceptado.
        OAuth2TokenValidator<Jwt> validadores = new DelegatingOAuth2TokenValidator<>(
                List.of(
                        new JwtTimestampValidator(),
                        new JwtIssuerValidator(emisorEsperado)
                ));

        decoder.setJwtValidator(validadores);
        return decoder;
    }
}
