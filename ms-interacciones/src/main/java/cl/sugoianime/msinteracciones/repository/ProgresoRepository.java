package cl.sugoianime.msinteracciones.repository;

import cl.sugoianime.msinteracciones.domain.ProgresoVisualizacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgresoRepository extends JpaRepository<ProgresoVisualizacion, Long> {

    Optional<ProgresoVisualizacion> findByPerfilIdAndEpisodioId(Long perfilId, Long episodioId);

    List<ProgresoVisualizacion> findByPerfilIdAndAnimeIdOrderByNumeroEpisodioAsc(
            Long perfilId, Long animeId);

    /** Fila "Continuar viendo": episodios empezados y no terminados. */
    @Query("""
        SELECT p FROM ProgresoVisualizacion p
        WHERE p.perfilId = :perfilId
          AND p.completado = false
          AND p.segundo > 30
        ORDER BY p.actualizadoEn DESC
        """)
    List<ProgresoVisualizacion> findContinuarViendo(Long perfilId, Pageable pageable);

    /** Animes con al menos un episodio visto: insumo de las recomendaciones. */
    @Query("""
        SELECT DISTINCT p.animeId FROM ProgresoVisualizacion p
        WHERE p.perfilId = :perfilId AND p.completado = true
        """)
    List<Long> findAnimeIdsVistos(Long perfilId);

    /** Todos los animes con los que el perfil interactuo, vistos o no. */
    @Query("SELECT DISTINCT p.animeId FROM ProgresoVisualizacion p WHERE p.perfilId = :perfilId")
    List<Long> findAnimeIdsConInteraccion(Long perfilId);

    @Query("SELECT DISTINCT p.perfilId FROM ProgresoVisualizacion p")
    List<Long> findPerfilesConActividad();

    void deleteByPerfilId(Long perfilId);
}
