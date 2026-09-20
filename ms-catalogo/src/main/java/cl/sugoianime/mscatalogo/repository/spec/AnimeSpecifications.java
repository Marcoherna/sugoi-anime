package cl.sugoianime.mscatalogo.repository.spec;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.domain.EstadoAnime;
import cl.sugoianime.mscatalogo.domain.Genero;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros componibles para el catalogo (RF-01).
 *
 * Usamos la Criteria API en vez de concatenar strings SQL:
 * los filtros se combinan dinamicamente y quedan inmunes a inyeccion SQL.
 */
public final class AnimeSpecifications {

    private AnimeSpecifications() {
        // clase de utilidad
    }

    public static Specification<Anime> conGenero(String nombreGenero) {
        return (root, query, cb) -> {
            if (nombreGenero == null || nombreGenero.isBlank()) {
                return null;
            }
            // distinct evita duplicados cuando un anime tiene varios generos
            if (query != null) {
                query.distinct(true);
            }
            Join<Anime, Genero> join = root.join("generos", JoinType.INNER);
            return cb.or(
                    cb.equal(cb.lower(join.get("nombre")),   nombreGenero.toLowerCase()),
                    cb.equal(cb.lower(join.get("nombreEs")), nombreGenero.toLowerCase())
            );
        };
    }

    public static Specification<Anime> conAnio(Integer anio) {
        return (root, query, cb) ->
                anio == null ? null : cb.equal(root.get("anio"), anio);
    }

    public static Specification<Anime> conAnioEntre(Integer desde, Integer hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) return null;
            if (desde == null) return cb.lessThanOrEqualTo(root.get("anio"), hasta);
            if (hasta == null) return cb.greaterThanOrEqualTo(root.get("anio"), desde);
            return cb.between(root.get("anio"), desde, hasta);
        };
    }

    public static Specification<Anime> conTituloParecidoA(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return null;
            String patron = "%" + texto.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("titulo")), patron),
                    cb.like(cb.lower(root.get("tituloJapones")), patron)
            );
        };
    }

    public static Specification<Anime> conEstado(EstadoAnime estado) {
        return (root, query, cb) ->
                estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    public static Specification<Anime> conPuntuacionMinima(Double minimo) {
        return (root, query, cb) ->
                minimo == null ? null
                        : cb.greaterThanOrEqualTo(root.get("puntuacion"), java.math.BigDecimal.valueOf(minimo));
    }

    /** Combina solo los filtros que vienen informados. */
    public static Specification<Anime> combinar(List<Specification<Anime>> specs) {
        List<Specification<Anime>> noNulos = new ArrayList<>();
        for (Specification<Anime> s : specs) {
            if (s != null) {
                noNulos.add(s);
            }
        }
        return noNulos.isEmpty() ? null : Specification.allOf(noNulos);
    }
}
