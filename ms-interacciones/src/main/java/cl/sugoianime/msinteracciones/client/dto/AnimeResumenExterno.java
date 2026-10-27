package cl.sugoianime.msinteracciones.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Proyeccion del AnimeResumenDTO que expone ms-catalogo.
 *
 * Se declara aparte a proposito: si compartieramos una libreria de DTOs
 * entre microservicios, un cambio en ms-catalogo obligaria a recompilar
 * y redesplegar ms-interacciones. Ese acoplamiento es exactamente lo que
 * los microservicios buscan evitar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimeResumenExterno(
        Long id,
        String titulo,
        Integer anio,
        BigDecimal puntuacion,
        String imagenUrl,
        List<GeneroExterno> generos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneroExterno(Long id, String nombre) {}

    public List<GeneroExterno> generosSeguro() {
        return generos == null ? Collections.emptyList() : generos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagina(
            List<AnimeResumenExterno> contenido,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean ultima
    ) {
        public List<AnimeResumenExterno> contenidoSeguro() {
            return contenido == null ? Collections.emptyList() : contenido;
        }
    }
}
