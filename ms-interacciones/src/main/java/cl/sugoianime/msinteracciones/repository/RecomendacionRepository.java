package cl.sugoianime.msinteracciones.repository;

import cl.sugoianime.msinteracciones.domain.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    List<Recomendacion> findByPerfilIdOrderByPosicionAsc(Long perfilId);

    void deleteByPerfilId(Long perfilId);
}
