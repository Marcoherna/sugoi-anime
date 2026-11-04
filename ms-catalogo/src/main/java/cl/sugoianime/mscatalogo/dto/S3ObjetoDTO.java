package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Archivo multimedia almacenado en el bucket S3")
public record S3ObjetoDTO(
        String key,
        long tamanoBytes,
        String ultimaModificacion,
        String urlPublica
) {}

