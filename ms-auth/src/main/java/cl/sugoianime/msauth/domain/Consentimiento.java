package cl.sugoianime.msauth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Registro auditable de consentimiento. Ley 19.628.
 *
 * Es INMUTABLE por diseno: no se actualiza ni se borra. Si el usuario
 * revoca un consentimiento, se inserta una fila nueva con aceptado=false.
 * Asi queda el historial completo de que acepto y cuando.
 */
@Entity
@Table(name = "consentimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimiento {

    public enum Tipo {
        TERMINOS_Y_CONDICIONES,
        TRATAMIENTO_DATOS,
        HISTORIAL_RECOMENDACIONES
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "version_terminos", nullable = false, length = 20)
    private String versionTerminos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Tipo tipo;

    @Column(nullable = false)
    private boolean aceptado;

    @CreationTimestamp
    @Column(name = "aceptado_en", nullable = false, updatable = false)
    private OffsetDateTime aceptadoEn;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;
}
