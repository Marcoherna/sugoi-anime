package cl.sugoianime.msauth.security;

import cl.sugoianime.msauth.config.JwtProperties;
import cl.sugoianime.msauth.domain.Rol;
import cl.sugoianime.msauth.domain.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties propiedades;
    private final SecureRandom random = new SecureRandom();

    public JwtService(JwtEncoder encoder, JwtProperties propiedades) {
        this.encoder = encoder;
        this.propiedades = propiedades;
    }

    /**
     * Genera el access token. Los claims que aqui se incluyen son los
     * que el API Gateway y los demas microservicios podran leer sin
     * consultar a ms-auth: ese es el sentido de una sesion sin estado.
     */
    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();

        List<String> roles = usuario.getRoles().stream().map(Rol::name).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.accessTokenDuracion()))
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombre())
                .claim("roles", roles)
                .claim("premium", usuario.isEsPremium())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Genera un refresh token OPACO (aleatorio), no un JWT.
     *
     * A proposito: un refresh token no necesita transportar informacion,
     * solo identificar una sesion en la base de datos. Si fuera un JWT
     * con claims, filtrarlo revelaria datos del usuario sin aportar nada.
     */
    public String generarRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public long accessTokenSegundos() {
        return propiedades.accessTokenDuracion().toSeconds();
    }
}
