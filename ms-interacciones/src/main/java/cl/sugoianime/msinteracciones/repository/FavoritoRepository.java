package cl.sugoianime.msinteracciones.repository;

import cl.sugoianime.msinteracciones.domain.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    Page<Favorito> findByPerfilIdOrderByCreadoEnDesc(Long perfilId, Pageable pageable);

    Optional<Favorito> findByPerfilIdAndAnimeId(Long perfilId, Long animeId);

    boolean existsByPerfilIdAndAnimeId(Long perfilId, Long animeId);

    long countByPerfilId(Long perfilId);

    /** Ids de animes favoritos, insumo del motor de recomendaciones. */
    @Query("SELECT f.animeId FROM Favorito f WHERE f.perfilId = :perfilId")
    List<Long> findAnimeIdsByPerfilId(Long perfilId);

    /** Limpieza de datos huerfanos tras eliminar un perfil en ms-auth. */
    void deleteByPerfilId(Long perfilId);
}
