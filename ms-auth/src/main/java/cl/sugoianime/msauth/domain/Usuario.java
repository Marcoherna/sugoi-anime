package cl.sugoianime.msauth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** Hash BCrypt. La contrasena en claro nunca toca la base de datos. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Lo actualiza ms-pagos mediante evento asincrono (RF-10, RNF-08). */
    @Column(name = "es_premium", nullable = false)
    @Builder.Default
    private boolean esPremium = false;

    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol", length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Rol> roles = EnumSet.of(Rol.USUARIO);

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Perfil> perfiles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public void agregarPerfil(Perfil perfil) {
        this.perfiles.add(perfil);
        perfil.setUsuario(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario otro)) return false;
        return email != null && email.equals(otro.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }
}
