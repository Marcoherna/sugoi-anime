package cl.sugoianime.mscatalogo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "genero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mal_id", nullable = false, unique = true)
    private Integer malId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_es", length = 100)
    private String nombreEs;

    @ManyToMany(mappedBy = "generos", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Anime> animes = new LinkedHashSet<>();

    /** Devuelve el nombre en espanol si existe, si no el original (RF-05). */
    public String nombreParaMostrar() {
        return nombreEs != null && !nombreEs.isBlank() ? nombreEs : nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genero otro)) return false;
        return malId != null && malId.equals(otro.malId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(malId);
    }
}
