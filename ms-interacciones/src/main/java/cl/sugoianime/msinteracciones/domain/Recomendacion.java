package cl.sugoianime.msinteracciones.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Recomendacion precalculada para un perfil (RF-07).
 *
 * Se calcula por job nocturno. Recomendar en tiempo de request obligaria
 * a traer el catalogo completo y calcular similitudes en cada carga del
 * home: inaceptable en latencia.
 */
@Entity
@Table(name = "recomendacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recomendacion_perfil_anime",
                columnNames = {"perfil_id", "anime_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_id", nullable = false)
    private Long perfilId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    /** Similitud de coseno, entre 0 y 1. */
    @Column(nullable = false)
    private Double puntaje;

    @Column(nullable = false)
    private Integer posicion;

    @CreationTimestamp
    @Column(name = "generado_en", nullable = false, updatable = false)
    private OffsetDateTime generadoEn;
}
