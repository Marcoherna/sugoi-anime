package cl.sugoianime.msinteracciones.repository;

import cl.sugoianime.msinteracciones.domain.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    List<Recomendacion> findByPerfilIdOrderByPosicionAsc(Long perfilId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Recomendacion r WHERE r.perfilId = :perfilId")
    void deleteByPerfilId(@Param("perfilId") Long perfilId);
}
