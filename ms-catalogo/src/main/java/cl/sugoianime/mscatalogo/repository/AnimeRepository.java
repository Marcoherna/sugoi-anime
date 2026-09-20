package cl.sugoianime.mscatalogo.repository;

import cl.sugoianime.mscatalogo.domain.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long>, JpaSpecificationExecutor<Anime>{
    Optional<Anime> findByMalId(Integer malId);

    boolean existsByMalId(Integer malId);

    /**
     * Carga el anime con sus generos y episodios en una sola consulta.
     * Sin el EntityGraph tendriamos el problema N+1.
     */
    @EntityGraph(attributePaths = {"generos", "episodiosLista"})
    Optional<Anime> findWithDetalleById(Long id);

    /** Animes pendientes de traduccion (RF-05). */
    @Query("""
        SELECT a FROM Anime a
        WHERE a.sinopsisEs IS NULL
          AND a.sinopsis IS NOT NULL
        ORDER BY a.puntuacion DESC NULLS LAST
        """)
    List<Anime> findPendientesDeTraduccion(Pageable pageable);

    /** Fila "Top valorados" del home. */
    @EntityGraph(attributePaths = "generos")
    Page<Anime> findByPuntuacionIsNotNullOrderByPuntuacionDesc(Pageable pageable);
}
