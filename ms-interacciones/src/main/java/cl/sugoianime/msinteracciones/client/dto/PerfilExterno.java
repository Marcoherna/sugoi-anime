package cl.sugoianime.msinteracciones.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfilExterno(
        Long id,
        String nombre,
        String avatar,
        boolean esInfantil
) {}
