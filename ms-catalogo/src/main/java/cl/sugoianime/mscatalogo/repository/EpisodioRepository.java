package cl.sugoianime.mscatalogo.repository;

import cl.sugoianime.mscatalogo.domain.Episodio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodioRepository extends JpaRepository<Episodio, Long> {
    List<Episodio> findByAnimeIdOrderByNumeroAsc(Long animeId);

    Optional<Episodio> findByAnimeIdAndNumero(Long animeId, Integer numero);
}
