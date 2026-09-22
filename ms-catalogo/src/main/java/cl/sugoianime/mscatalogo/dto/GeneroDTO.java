package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Genero de un anime")
public record GeneroDTO(

        @Schema(description = "Identificador interno", example = "1")
        Long id,

        @Schema(description = "Nombre para mostrar, en espanol si esta disponible",
                example = "Accion")
        String nombre
) {}
