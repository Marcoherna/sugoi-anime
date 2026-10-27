package cl.sugoianime.msinteracciones.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Punto exacto de reproduccion de un episodio (RF-03).
 *
 * Un unico registro por perfil y episodio: se SOBREESCRIBE en cada
 * actualizacion. No guardamos historial de posiciones porque no aporta
 * nada al caso de uso y multiplicaria las filas por cada 10 segundos
 * de reproduccion.
 */
@Entity
@Table(name = "progreso_visualizacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_progreso_perfil_episodio",
                columnNames = {"perfil_id", "episodio_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgresoVisualizacion {

    /** A partir de este porcentaje se considera el episodio como visto. */
    public static final double UMBRAL_COMPLETADO = 0.90;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_id", nullable = false)
    private Long perfilId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @Column(name = "episodio_id", nullable = false)
    private Long episodioId;

    @Column(name = "numero_episodio")
    private Integer numeroEpisodio;

    @Column(nullable = false)
    @Builder.Default
    private Integer segundo = 0;

    @Column(name = "duracion_seg")
    private Integer duracionSeg;

    @Column(nullable = false)
    @Builder.Default
    private boolean completado = false;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    /**
     * Recalcula el flag de completado.
     *
     * Se usa un 90% en vez del 100% porque casi nadie ve los creditos
     * finales: exigir el segundo exacto dejaria episodios vistos como
     * "pendientes" para siempre.
     */
    public void recalcularCompletado() {
        if (duracionSeg != null && duracionSeg > 0) {
            this.completado = ((double) segundo / duracionSeg) >= UMBRAL_COMPLETADO;
        }
    }

    public int porcentaje() {
        if (duracionSeg == null || duracionSeg == 0) return 0;
        return (int) Math.min(100, Math.round((double) segundo / duracionSeg * 100));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgresoVisualizacion otro)) return false;
        return Objects.equals(perfilId, otro.perfilId)
                && Objects.equals(episodioId, otro.episodioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perfilId, episodioId);
    }
}
