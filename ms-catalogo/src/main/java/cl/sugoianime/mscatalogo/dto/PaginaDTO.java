package cl.sugoianime.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltorio de paginacion propio.
 *
 * Devolver Page<T> de Spring Data directamente genera un contrato JSON
 * inestable entre versiones (y Boot avisa por log). Con este record el
 * contrato de la API es nuestro y no cambia si actualizamos Spring.
 */
@Schema(description = "Resultado paginado")
public record PaginaDTO<T>(

        @Schema(description = "Elementos de la pagina actual")
        List<T> contenido,

        @Schema(description = "Numero de pagina, base 0", example = "0")
        int pagina,

        @Schema(description = "Tamano de pagina solicitado", example = "20")
        int tamano,

        @Schema(description = "Total de elementos que cumplen el filtro", example = "137")
        long totalElementos,

        @Schema(description = "Total de paginas disponibles", example = "7")
        int totalPaginas,

        @Schema(description = "Indica si esta es la ultima pagina", example = "false")
        boolean ultima
) {

    public static <E, T> PaginaDTO<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PaginaDTO<>(
                page.getContent().stream().map(mapeador).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
