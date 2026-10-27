package cl.sugoianime.msinteracciones.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Anime marcado como favorito por un perfil (RF-06).
 *
 * perfilId y animeId son referencias LOGICAS a otras bases de datos.
 * No hay @ManyToOne ni FK: seria imposible entre instancias PostgreSQL
 * distintas, y ademas acoplaria los esquemas de los microservicios.
 */
@Entity
@Table(name = "favorito",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_favorito_perfil_anime",
                columnNames = {"perfil_id", "anime_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_id", nullable = false)
    private Long perfilId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Favorito otro)) return false;
        return Objects.equals(perfilId, otro.perfilId)
                && Objects.equals(animeId, otro.animeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perfilId, animeId);
    }
}
