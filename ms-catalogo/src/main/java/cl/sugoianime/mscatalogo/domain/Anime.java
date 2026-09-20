package cl.sugoianime.mscatalogo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "anime")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mal_id", nullable = false, unique = true)
    private Integer malId;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(name = "titulo_japones", length = 500)
    private String tituloJapones;

    @Column(columnDefinition = "TEXT")
    private String sinopsis;

    /** Traduccion cacheada en BD. RF-05: evita llamar a la API en cada request. */
    @Column(name = "sinopsis_es", columnDefinition = "TEXT")
    private String sinopsisEs;

    private Integer anio;

    private Integer episodios;

    @Column(precision = 4, scale = 2)
    private BigDecimal puntuacion;

    /** Key del objeto en S3. NO guardamos la URL completa (RNF-07). */
    @Column(name = "imagen_key", length = 1000)
    private String imagenKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoAnime estado = EstadoAnime.DESCONOCIDO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "anime_genero",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "genero_id")
    )
    @Builder.Default
    private Set<Genero> generos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "anime", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    @Builder.Default
    private Set<Episodio> episodiosLista = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    // ---- Helpers de relacion (mantienen ambos lados sincronizados) ----

    public void agregarGenero(Genero genero) {
        this.generos.add(genero);
        genero.getAnimes().add(this);
    }

    public void agregarEpisodio(Episodio episodio) {
        this.episodiosLista.add(episodio);
        episodio.setAnime(this);
    }

    // ---- equals/hashCode basados en la clave de negocio, no en el id ----
    // Necesario porque las entidades viven dentro de un Set.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Anime otro)) return false;
        return malId != null && malId.equals(otro.malId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(malId);
    }
}
