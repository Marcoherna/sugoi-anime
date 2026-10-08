package cl.sugoianime.mscatalogo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de entrega de medios (RNF-07).
 *
 * Es un record con constructor binding: las propiedades quedan
 * inmutables y validadas al arrancar.
 */
@ConfigurationProperties(prefix = "media")
public record StreamingProperties(
        String cdnDomain,
        String baseUrlLocal
) {

    public boolean cdnDisponible() {
        return cdnDomain != null && !cdnDomain.isBlank();
    }
}
