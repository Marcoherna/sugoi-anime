package cl.sugoianime.mscatalogo.repository;

import cl.sugoianime.mscatalogo.domain.Genero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneroRepository extends JpaRepository<Genero, Long> {
    Optional<Genero> findByMalId(Integer malId);

    Optional<Genero> findByNombreIgnoreCase(String nombre);

    List<Genero> findAllByOrderByNombreAsc();
}
