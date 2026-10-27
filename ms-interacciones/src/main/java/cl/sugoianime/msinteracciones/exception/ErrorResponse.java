package cl.sugoianime.msinteracciones.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Estructura estandar de error")
public record ErrorResponse(
        OffsetDateTime momento,
        int estado,
        String error,
        String mensaje,
        String ruta,
        List<String> detalles
) {

    public static ErrorResponse de(int estado, String error, String mensaje, String ruta) {
        return new ErrorResponse(OffsetDateTime.now(), estado, error, mensaje, ruta, null);
    }
}
