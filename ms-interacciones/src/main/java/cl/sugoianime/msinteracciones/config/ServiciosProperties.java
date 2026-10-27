package cl.sugoianime.msinteracciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "servicios")
public record ServiciosProperties(
        String catalogoUrl,
        String authUrl
) {}
