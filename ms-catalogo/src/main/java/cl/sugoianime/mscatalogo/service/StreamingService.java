package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.config.StreamingProperties;
import cl.sugoianime.mscatalogo.domain.Episodio;
import cl.sugoianime.mscatalogo.dto.ReproduccionDTO;
import cl.sugoianime.mscatalogo.exception.RecursoNoEncontradoException;
import cl.sugoianime.mscatalogo.repository.EpisodioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega de contenido multimedia (RF-02, RNF-07).
 *
 * Decision de diseno: en base de datos guardamos solo la KEY del objeto
 * (ej. "demo/video.mp4"), nunca la URL completa. Asi, cambiar de CDN o de
 * bucket es un cambio de configuracion, no una migracion de datos.
 */
@Service
public class StreamingService {

    private final StreamingProperties propiedades;
    private final EpisodioRepository episodioRepository;

    public StreamingService(StreamingProperties propiedades,
                            EpisodioRepository episodioRepository) {
        this.propiedades = propiedades;
        this.episodioRepository = episodioRepository;
    }

    /** Convierte una key de S3 en URL publica servida por CloudFront. */
    public String construirUrlPublica(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        // Las portadas provenientes de Jikan ya son URLs completas.
        // Anteponerles el dominio del CDN produciria una URL invalida.
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        String base = propiedades.cdnDisponible()
                ? "https://" + propiedades.cdnDomain()
                : propiedades.baseUrlLocal();
        return base + "/" + key;
    }

    @Transactional(readOnly = true)
    public ReproduccionDTO prepararReproduccion(Long episodioId) {
        Episodio episodio = episodioRepository.findById(episodioId)
                .orElseThrow(() -> RecursoNoEncontradoException.episodio(episodioId));

        return new ReproduccionDTO(
                episodio.getId(),
                episodio.getAnime().getId(),
                episodio.getAnime().getTitulo(),
                episodio.getNumero(),
                construirUrlPublica(episodio.getVideoKey()),
                episodio.getDuracionSeg(),
                episodio.getLicencia()
        );
    }
}
