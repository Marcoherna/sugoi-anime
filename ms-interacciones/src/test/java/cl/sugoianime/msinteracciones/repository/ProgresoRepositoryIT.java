package cl.sugoianime.msinteracciones.repository;

import cl.sugoianime.msinteracciones.domain.ProgresoVisualizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("ProgresoRepository contra PostgreSQL real (valida RNF-02)")
class ProgresoRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private ProgresoRepository progresoRepository;

    @Test
    @DisplayName("un episodio con menos de 30 segundos no aparece en continuar viendo")
    void continuarViendo_filtraAperturaAccidental() {
        progresoRepository.save(ProgresoVisualizacion.builder()
                .perfilId(100L).usuarioId(1L).animeId(1L).episodioId(1L)
                .segundo(15).duracionSeg(1440).build());

        var resultado = progresoRepository.findContinuarViendo(100L, PageRequest.of(0, 10));

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("un episodio a medias si aparece en continuar viendo")
    void continuarViendo_incluyeEpisodioAMedias() {
        progresoRepository.save(ProgresoVisualizacion.builder()
                .perfilId(101L).usuarioId(1L).animeId(1L).episodioId(10L)
                .segundo(700).duracionSeg(1440).build());

        var resultado = progresoRepository.findContinuarViendo(101L, PageRequest.of(0, 10));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getSegundo()).isEqualTo(700);
    }

    @Test
    @DisplayName("un episodio completado desaparece de continuar viendo")
    void continuarViendo_excluyeCompletados() {
        ProgresoVisualizacion p = ProgresoVisualizacion.builder()
                .perfilId(102L).usuarioId(1L).animeId(1L).episodioId(20L)
                .segundo(1400).duracionSeg(1440).build();
        p.recalcularCompletado();
        progresoRepository.save(p);

        assertThat(p.isCompletado()).isTrue();
        assertThat(progresoRepository.findContinuarViendo(102L, PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    @DisplayName("guardar dos veces el mismo episodio actualiza, no duplica")
    void guardar_esIdempotente() {
        progresoRepository.save(ProgresoVisualizacion.builder()
                .perfilId(103L).usuarioId(1L).animeId(1L).episodioId(30L)
                .segundo(100).duracionSeg(1440).build());

        var existente = progresoRepository
                .findByPerfilIdAndEpisodioId(103L, 30L).orElseThrow();
        existente.setSegundo(500);
        progresoRepository.save(existente);

        assertThat(progresoRepository.findByPerfilIdAndAnimeIdOrderByNumeroEpisodioAsc(103L, 1L))
                .hasSize(1);
        assertThat(progresoRepository.findByPerfilIdAndEpisodioId(103L, 30L)
                .orElseThrow().getSegundo()).isEqualTo(500);
    }

    @Test
    @DisplayName("el umbral de completado es el 90%")
    void umbralCompletado() {
        ProgresoVisualizacion casi = ProgresoVisualizacion.builder()
                .perfilId(104L).usuarioId(1L).animeId(1L).episodioId(40L)
                .segundo(1290).duracionSeg(1440).build();   // 89.6%
        casi.recalcularCompletado();
        assertThat(casi.isCompletado()).isFalse();

        casi.setSegundo(1300);   // 90.3%
        casi.recalcularCompletado();
        assertThat(casi.isCompletado()).isTrue();
    }
}
