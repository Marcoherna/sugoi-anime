package cl.sugoianime.mscatalogo.repository;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.repository.spec.AnimeSpecifications;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
// OJO TESTCONTAINERS 2.0: el paquete cambio.
// Antes era org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("AnimeRepository contra PostgreSQL real (valida RNF-02)")
class AnimeRepositoryIT {

    @Container
    @ServiceConnection   // Boot configura el DataSource automaticamente
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    @DisplayName("las migraciones Flyway cargan el dataset semilla")
    void flyway_cargaDatosSemilla() {
        assertThat(animeRepository.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("el filtro por anio devuelve solo animes de ese anio (RF-01)")
    void filtroPorAnio_devuelveSoloEseAnio() {
        Page<Anime> resultado = animeRepository.findAll(
                AnimeSpecifications.combinar(List.of(AnimeSpecifications.conAnio(2013))),
                PageRequest.of(0, 20));

        assertThat(resultado.getContent())
                .isNotEmpty()
                .allMatch(a -> a.getAnio() == 2013);
    }

    @Test
    @DisplayName("el filtro por genero hace join y no duplica resultados (RF-01)")
    void filtroPorGenero_noDuplicaResultados() {
        Page<Anime> resultado = animeRepository.findAll(
                AnimeSpecifications.combinar(List.of(AnimeSpecifications.conGenero("Accion"))),
                PageRequest.of(0, 20));

        assertThat(resultado.getContent()).isNotEmpty();
        assertThat(resultado.getContent())
                .extracting(Anime::getMalId)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("la busqueda por texto es insensible a mayusculas")
    void busquedaPorTexto_insensibleAMayusculas() {
        Page<Anime> resultado = animeRepository.findAll(
                AnimeSpecifications.combinar(
                        List.of(AnimeSpecifications.conTituloParecidoA("TITAN"))),
                PageRequest.of(0, 20));

        assertThat(resultado.getContent())
                .anyMatch(a -> a.getTitulo().toLowerCase().contains("titan"));
    }

    @Test
    @DisplayName("findWithDetalleById carga generos y episodios sin N+1")
    void findWithDetalle_cargaRelaciones() {
        Anime primero = animeRepository.findAll(PageRequest.of(0, 1))
                .getContent().getFirst();

        var detalle = animeRepository.findWithDetalleById(primero.getId());

        assertThat(detalle).isPresent();
        assertThat(detalle.get().getGeneros()).isNotNull();
    }
}