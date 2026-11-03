package cl.sugoianime.msauth.repository;

import cl.sugoianime.msauth.domain.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    List<Perfil> findByUsuarioIdOrderByCreadoEnAsc(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    Optional<Perfil> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);
}
