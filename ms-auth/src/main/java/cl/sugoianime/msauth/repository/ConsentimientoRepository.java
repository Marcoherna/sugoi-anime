package cl.sugoianime.msauth.repository;

import cl.sugoianime.msauth.domain.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    List<Consentimiento> findByUsuarioIdOrderByAceptadoEnDesc(Long usuarioId);
}
