package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.dto.AnimeDetalleDTO;
import cl.sugoianime.mscatalogo.dto.AnimeResumenDTO;
import cl.sugoianime.mscatalogo.dto.GeneroDTO;
import cl.sugoianime.mscatalogo.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogoService {

    /** RF-01: catalogo paginado con filtros opcionales. */
    PaginaDTO<AnimeResumenDTO> buscar(String genero, Integer anio, String texto, Pageable pageable);

    AnimeDetalleDTO obtenerDetalle(Long id);

    List<GeneroDTO> listarGeneros();

    PaginaDTO<AnimeResumenDTO> topValorados(Pageable pageable);
}
