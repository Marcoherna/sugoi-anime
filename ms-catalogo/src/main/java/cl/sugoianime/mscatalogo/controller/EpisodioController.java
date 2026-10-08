package cl.sugoianime.mscatalogo.controller;

import cl.sugoianime.mscatalogo.dto.EpisodioDTO;
import cl.sugoianime.mscatalogo.dto.ReproduccionDTO;
import cl.sugoianime.mscatalogo.exception.ErrorResponse;
import cl.sugoianime.mscatalogo.mapper.AnimeMapper;
import cl.sugoianime.mscatalogo.repository.EpisodioRepository;
import cl.sugoianime.mscatalogo.service.StreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/episodios")
@Tag(name = "Reproduccion",
        description = "Entrega de contenido multimedia desde el CDN (RF-02, RNF-07)")
public class EpisodioController {

    private final StreamingService streamingService;
    private final EpisodioRepository episodioRepository;
    private final AnimeMapper mapper;

    public EpisodioController(StreamingService streamingService,
                              EpisodioRepository episodioRepository,
                              AnimeMapper mapper) {
        this.streamingService = streamingService;
        this.episodioRepository = episodioRepository;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Obtener los datos de reproduccion de un episodio",
            description = """
            Implementa el requerimiento **RF-02**.

            Devuelve la URL del video servida por CloudFront (RNF-07) junto con \
            la duracion y la licencia del contenido.

            El campo `licencia` es parte del cumplimiento de la Ley 17.336: \
            todo contenido servido declara explicitamente su origen legal.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de reproduccion listos"),
            @ApiResponse(responseCode = "404", description = "El episodio no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/reproduccion")
    public ResponseEntity<ReproduccionDTO> reproducir(
            @Parameter(description = "Identificador del episodio", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(streamingService.prepararReproduccion(id));
    }

    @Operation(summary = "Listar los episodios disponibles de un anime")
    @GetMapping
    public ResponseEntity<List<EpisodioDTO>> porAnime(
            @Parameter(description = "Identificador del anime", example = "1")
            @RequestParam Long animeId) {

        List<EpisodioDTO> episodios = episodioRepository
                .findByAnimeIdOrderByNumeroAsc(animeId).stream()
                .map(mapper::aEpisodioDTO)
                .toList();

        return ResponseEntity.ok(episodios);
    }
}
