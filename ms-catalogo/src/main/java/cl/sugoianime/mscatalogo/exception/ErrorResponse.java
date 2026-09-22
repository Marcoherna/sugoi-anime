package cl.sugoianime.mscatalogo.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Estructura estandar de error de la API")
public record ErrorResponse(

        @Schema(example = "2026-09-05T14:32:11Z")
        OffsetDateTime momento,

        @Schema(example = "404")
        int estado,

        @Schema(example = "Not Found")
        String error,

        @Schema(example = "No existe un anime con id 999")
        String mensaje,

        @Schema(example = "/animes/999")
        String ruta,

        @Schema(description = "Detalle por campo, solo en errores de validacion")
        List<String> detalles
) {

    public static ErrorResponse de(int estado, String error, String mensaje, String ruta) {
        return new ErrorResponse(OffsetDateTime.now(), estado, error, mensaje, ruta, null);
    }
}
