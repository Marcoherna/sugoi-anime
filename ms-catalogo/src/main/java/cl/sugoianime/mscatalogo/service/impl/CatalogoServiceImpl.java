package cl.sugoianime.mscatalogo.service.impl;

import cl.sugoianime.mscatalogo.domain.Anime;
import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.AnimeResumenDTO;
import cl.sugoianime.mscatalogo.dto.GeneroDTO;
import cl.sugoianime.mscatalogo.dto.PaginaDTO;
import cl.sugoianime.mscatalogo.exception.RecursoNoEncontradoException;
import cl.sugoianime.mscatalogo.mapper.AnimeMapper;
import cl.sugoianime.mscatalogo.repository.AnimeRepository;
import cl.sugoianime.mscatalogo.repository.GeneroRepository;
import cl.sugoianime.mscatalogo.repository.spec.AnimeSpecifications;
import cl.sugoianime.mscatalogo.service.CatalogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoServiceImpl.class);

    private final AnimeRepository animeRepository;
    private final GeneroRepository generoRepository;
    private final AnimeMapper mapper;

    public CatalogoServiceImpl(AnimeRepository animeRepository,
                               GeneroRepository generoRepository,
                               AnimeMapper mapper) {
        this.animeRepository = animeRepository;
        this.generoRepository = generoRepository;
        this.mapper = mapper;
    }

    @Override
    public PaginaDTO<AnimeResumenDTO> buscar(String genero, Integer anio,
                                             String texto, Pageable pageable) {

        log.debug("Buscando catalogo -> genero={}, anio={}, texto={}", genero, anio, texto);

        Specification<Anime> spec = AnimeSpecifications.combinar(List.of(
                AnimeSpecifications.conGenero(genero),
                AnimeSpecifications.conAnio(anio),
                AnimeSpecifications.conTituloParecidoA(texto)
        ));

        Page<Anime> resultado = animeRepository.findAll(spec, pageable);

        return PaginaDTO.de(resultado, mapper::aResumenDTO);
    }

    @Override
    public AnimeDetalleDTO obtenerDetalle(Long id) {
        Anime anime = animeRepository.findWithDetalleById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.anime(id));

        return mapper.aDetalleDTO(anime);
    }

    @Override
    public List<GeneroDTO> listarGeneros() {
        return generoRepository.findAllByOrderByNombreAsc().stream()
                .map(mapper::aGeneroDTO)
                .toList();
    }

    @Override
    public PaginaDTO<AnimeResumenDTO> topValorados(Pageable pageable) {
        Page<Anime> page = animeRepository
                .findByPuntuacionIsNotNullOrderByPuntuacionDesc(pageable);

        return PaginaDTO.de(page, mapper::aResumenDTO);
    }
}
