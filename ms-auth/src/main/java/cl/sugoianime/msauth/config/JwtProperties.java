package cl.sugoianime.msauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String emisor,
        Duration accessTokenDuracion,
        Duration refreshTokenDuracion
) {}
