package cl.sugoianime.mscatalogo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "episodio",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_episodio_anime_numero",
                columnNames = {"anime_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episodio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column(nullable = false)
    private Integer numero;

    @Column(length = 500)
    private String titulo;

    @Column(name = "duracion_seg")
    private Integer duracionSeg;

    /** Key del video en S3. RNF-07. */
    @Column(name = "video_key", length = 1000)
    private String videoKey;

    /**
     * Licencia del contenido. Trazabilidad de la Ley 17.336 (riesgo R04).
     * Valores: CC-BY-3.0, CC0, DOMINIO_PUBLICO, TRAILER_OFICIAL.
     */
    @Column(length = 200)
    private String licencia;

    /** URL original de descarga, para auditoria de derechos de autor. */
    @Column(name = "fuente_url", length = 1000)
    private String fuenteUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Episodio otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
