import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import Filtros from '../components/Filtros'
import AnimeCard from '../components/AnimeCard'
import Cargando from '../components/Cargando'

export default function Catalogo() {
  const [parametrosUrl, setParametrosUrl] = useSearchParams()

  // Los filtros viven en la URL: asi el usuario puede compartir el
  // enlace de una busqueda y el boton "atras" del navegador funciona.
  const filtros = {
    genero: parametrosUrl.get('genero') || '',
    anio:   parametrosUrl.get('anio') || '',
    texto:  parametrosUrl.get('texto') || '',
    page:   Number(parametrosUrl.get('page') || 0),
    size:   20,
    sort:   parametrosUrl.get('sort') || 'puntuacion,desc',
  }

  const [pagina, setPagina] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    setCargando(true)

    catalogoApi.listar(filtros)
        .then((datos) => { if (activo) { setPagina(datos); setError(null) } })
        .catch((ex) => { if (activo) setError(ex.message) })
        .finally(() => { if (activo) setCargando(false) })

    return () => { activo = false }
  }, [filtros.genero, filtros.anio, filtros.texto, filtros.page, filtros.sort])

  function cambiarFiltros(nuevos) {
    const limpios = {}
    Object.entries(nuevos).forEach(([clave, valor]) => {
      if (valor !== '' && valor !== null && valor !== undefined) {
        limpios[clave] = String(valor)
      }
    })
    setParametrosUrl(limpios)
  }

  return (
      <main className="contenedor pagina">
        <h1>Catálogo</h1>

        <Filtros valores={filtros} onCambio={cambiarFiltros} />

        {error && <div className="alerta alerta--error">{error}</div>}

        {cargando ? (
            <Cargando texto="Buscando animes..." />
        ) : pagina?.contenido?.length > 0 ? (
            <>
              <p className="texto-tenue mb-6">
                {pagina.totalElementos} resultado{pagina.totalElementos !== 1 ? 's' : ''}
              </p>

              <div className="grilla-animes">
                {pagina.contenido.map((anime) => (
                    <AnimeCard key={anime.id} anime={anime} />
                ))}
              </div>

              {pagina.totalPaginas > 1 && (
                  <div className="paginacion">
                    <button className="boton boton--secundario"
                            disabled={filtros.page === 0}
                            onClick={() => cambiarFiltros({ ...filtros, page: filtros.page - 1 })}>
                      Anterior
                    </button>
                    <span className="texto-suave">
                Página {pagina.pagina + 1} de {pagina.totalPaginas}
              </span>
                    <button className="boton boton--secundario"
                            disabled={pagina.ultima}
                            onClick={() => cambiarFiltros({ ...filtros, page: filtros.page + 1 })}>
                      Siguiente
                    </button>
                  </div>
              )}
            </>
        ) : (
            <div className="vacio">
              <p>No se encontraron animes con esos filtros.</p>
              <button className="boton boton--secundario"
                      onClick={() => cambiarFiltros({ sort: 'puntuacion,desc' })}>
                Ver todo el catálogo
              </button>
            </div>
        )}
      </main>
  )
}
