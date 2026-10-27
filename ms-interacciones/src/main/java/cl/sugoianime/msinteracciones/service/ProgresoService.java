package cl.sugoianime.msinteracciones.service;

import cl.sugoianime.msinteracciones.client.CatalogoClient;
import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import cl.sugoianime.msinteracciones.domain.ProgresoVisualizacion;
import cl.sugoianime.msinteracciones.dto.ContinuarViendoDTO;
import cl.sugoianime.msinteracciones.dto.GuardarProgresoRequest;
import cl.sugoianime.msinteracciones.dto.ProgresoDTO;
import cl.sugoianime.msinteracciones.repository.ProgresoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ProgresoService {

    private static final Logger log = LoggerFactory.getLogger(ProgresoService.class);

    private final ProgresoRepository progresoRepository;
    private final CatalogoClient catalogoClient;

    public ProgresoService(ProgresoRepository progresoRepository,
                           CatalogoClient catalogoClient) {
        this.progresoRepository = progresoRepository;
        this.catalogoClient = catalogoClient;
    }

    /**
     * Guarda o actualiza el punto de reproduccion (RF-03).
     *
     * Es idempotente: un registro por perfil y episodio, que se
     * sobreescribe. El frontend llama a este endpoint cada 10 segundos,
     * asi que la operacion debe ser barata y no acumular filas.
     */
    @Transactional
    public ProgresoDTO guardar(Long usuarioId, GuardarProgresoRequest request) {

        ProgresoVisualizacion progreso = progresoRepository
                .findByPerfilIdAndEpisodioId(request.perfilId(), request.episodioId())
                .orElseGet(() -> ProgresoVisualizacion.builder()
                        .perfilId(request.perfilId())
                        .usuarioId(usuarioId)
                        .animeId(request.animeId())
                        .episodioId(request.episodioId())
                        .build());

        progreso.setNumeroEpisodio(request.numeroEpisodio());
        progreso.setSegundo(request.segundo());

        if (request.duracionSeg() != null) {
            progreso.setDuracionSeg(request.duracionSeg());
        }

        progreso.recalcularCompletado();

        progresoRepository.save(progreso);

        return mapear(progreso);
    }

    @Transactional(readOnly = true)
    public ProgresoDTO obtener(Long perfilId, Long episodioId) {
        return progresoRepository.findByPerfilIdAndEpisodioId(perfilId, episodioId)
                .map(this::mapear)
                // Sin progreso previo, el episodio empieza en el segundo 0.
                // Devolver 404 obligaria al frontend a manejar un caso normal
                // como si fuera un error.
                .orElse(new ProgresoDTO(null, episodioId, null, 0, null, 0, false, null));
    }

    @Transactional(readOnly = true)
    public List<ProgresoDTO> porAnime(Long perfilId, Long animeId) {
        return progresoRepository
                .findByPerfilIdAndAnimeIdOrderByNumeroEpisodioAsc(perfilId, animeId)
                .stream().map(this::mapear).toList();
    }

    /** Fila "Continuar viendo" del home. */
    @Transactional(readOnly = true)
    public List<ContinuarViendoDTO> continuarViendo(Long perfilId, int limite) {

        List<ProgresoVisualizacion> pendientes = progresoRepository
                .findContinuarViendo(perfilId, PageRequest.of(0, limite));

        List<Long> animeIds = pendientes.stream()
                .map(ProgresoVisualizacion::getAnimeId).distinct().toList();

        Map<Long, AnimeResumenExterno> animes = catalogoClient.obtenerAnimes(animeIds);

        return pendientes.stream().map(p -> {
            AnimeResumenExterno anime = animes.get(p.getAnimeId());
            return new ContinuarViendoDTO(
                    p.getAnimeId(),
                    anime == null ? null : anime.titulo(),
                    anime == null ? null : anime.imagenUrl(),
                    p.getEpisodioId(),
                    p.getNumeroEpisodio(),
                    p.getSegundo(),
                    p.porcentaje());
        }).toList();
    }

    private ProgresoDTO mapear(ProgresoVisualizacion p) {
        return new ProgresoDTO(
                p.getAnimeId(), p.getEpisodioId(), p.getNumeroEpisodio(),
                p.getSegundo(), p.getDuracionSeg(), p.porcentaje(),
                p.isCompletado(), p.getActualizadoEn());
    }
}
