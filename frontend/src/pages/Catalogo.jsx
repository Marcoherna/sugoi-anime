import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import Filtros from '../components/Filtros'
import AnimeCard from '../components/AnimeCard'
import Cargando from '../components/Cargando'

export default function Catalogo() {
    const navegar = useNavigate()
    const [filtros, setFiltros] = useState({ page: 0, size: 20, sort: 'puntuacion,desc' })
    const [pagina, setPagina] = useState(null)
    const [cargando, setCargando] = useState(true)
    const [vistaLista, setVistaLista] = useState(false)

    useEffect(() => {
        setCargando(true)
        catalogoApi
            .listar(filtros)
            .then(setPagina)
            .catch(() => setPagina({ contenido: [], totalPaginas: 0, totalElementos: 0 }))
            .finally(() => setCargando(false))
    }, [filtros.texto, filtros.genero, filtros.anio, filtros.sort, filtros.page])

    const lista = pagina?.contenido || pagina?.content || []

    return (
        <main className="pt-28 md:pt-24 pb-12 max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop w-full">
            {/* Ambient Background Glow */}
            <div className="fixed top-16 left-1/2 -translate-x-1/2 w-[800px] h-[320px] bg-primary/5 blur-[120px] rounded-full pointer-events-none -z-10" />

            {/* Catalog Header */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-2 mb-6">
                <div>
                    <span className="text-label-sm uppercase tracking-widest text-primary block mb-1">
                        Directorio Completo
                    </span>
                    <h1 className="font-headline-xl text-2xl md:text-3xl font-extrabold text-on-surface tracking-tight">
                        Catálogo de Anime
                    </h1>
                </div>
                {pagina && (
                    <p className="text-caption text-on-surface-variant">
                        Mostrando <span className="text-on-surface font-semibold">{pagina.totalElementos}</span>{' '}
                        {pagina.totalElementos === 1 ? 'resultado' : 'resultados'} disponibles en español
                    </p>
                )}
            </div>

            <Filtros valores={filtros} onCambio={setFiltros} />

            {/* Active Status Bar */}
            {pagina && (
                <div className="flex items-center justify-between mb-6 px-1">
                    <div className="flex items-center gap-2">
                        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                        <span className="text-label-md text-on-surface-variant">
                            <strong className="text-on-surface">{pagina.totalElementos}</strong> resultados encontrados
                        </span>
                    </div>
                    <div className="flex items-center gap-1 text-caption text-outline">
                        <button
                            type="button"
                            onClick={() => setVistaLista(false)}
                            aria-label="Vista de cuadrícula"
                            className={`p-1.5 rounded transition-colors ${
                                !vistaLista
                                    ? 'bg-surface-container-high text-primary'
                                    : 'hover:bg-surface-container-low text-outline hover:text-on-surface'
                            }`}
                        >
                            <span className="material-symbols-outlined text-lg">grid_view</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => setVistaLista(true)}
                            aria-label="Vista de lista"
                            className={`p-1.5 rounded transition-colors ${
                                vistaLista
                                    ? 'bg-surface-container-high text-primary'
                                    : 'hover:bg-surface-container-low text-outline hover:text-on-surface'
                            }`}
                        >
                            <span className="material-symbols-outlined text-lg">view_list</span>
                        </button>
                    </div>
                </div>
            )}

            {cargando ? (
                <Cargando texto="Buscando animes..." />
            ) : lista.length === 0 ? (
                <div className="bg-surface-container-low border border-outline-variant/25 rounded-2xl p-12 text-center">
                    <span className="material-symbols-outlined text-5xl text-outline mb-3">search_off</span>
                    <h2 className="font-title-card text-lg font-bold text-on-surface mb-1">
                        No se encontraron animes con esos filtros
                    </h2>
                    <p className="text-caption text-on-surface-variant">
                        Prueba limpiando la búsqueda o eligiendo otro género o año.
                    </p>
                </div>
            ) : !vistaLista ? (
                <section
                    aria-label="Resultados del catálogo"
                    className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-gutter lg:gap-gutter-desktop"
                >
                    {lista.map((anime) => (
                        <AnimeCard key={anime.id} anime={anime} />
                    ))}
                </section>
            ) : (
                <section aria-label="Resultados del catálogo en lista" className="flex flex-col gap-3">
                    {lista.map((anime) => (
                        <div
                            key={anime.id}
                            onClick={() => navegar(`/anime/${anime.id}`)}
                            className="group flex items-center justify-between gap-4 p-3.5 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/20 hover:border-primary/40 cursor-pointer transition-all"
                        >
                            <div className="flex items-center gap-4 min-w-0">
                                <img
                                    src={anime.imagenUrl}
                                    alt={anime.titulo}
                                    className="w-14 h-20 object-cover rounded-lg bg-surface-container-highest shrink-0"
                                />
                                <div className="min-w-0">
                                    <h3 className="font-title-card text-base font-bold text-on-surface group-hover:text-primary transition-colors truncate">
                                        {anime.titulo}
                                    </h3>
                                    <p className="text-caption text-on-surface-variant line-clamp-2 mt-1">
                                        {anime.sinopsis}
                                    </p>
                                    <div className="flex flex-wrap items-center gap-2 mt-2">
                                        {anime.anio && (
                                            <span className="text-xs text-outline font-medium">{anime.anio}</span>
                                        )}
                                        {anime.generos?.slice(0, 4).map((g) => (
                                            <span
                                                key={g.id ?? g}
                                                className="px-2 py-0.5 rounded bg-surface-container-high text-[11px] text-on-surface-variant"
                                            >
                                                {g.nombre ?? g}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            </div>
                            {anime.puntuacion && (
                                <div className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-tertiary/15 text-tertiary font-bold text-sm shrink-0">
                                    <span
                                        className="material-symbols-outlined text-base"
                                        style={{ fontVariationSettings: "'FILL' 1" }}
                                    >
                                        star
                                    </span>
                                    <span>{Number(anime.puntuacion).toFixed(1)}</span>
                                </div>
                            )}
                        </div>
                    ))}
                </section>
            )}

            {pagina && pagina.totalPaginas > 1 && (
                <div className="mt-10 flex items-center justify-center gap-4">
                    <button
                        type="button"
                        disabled={filtros.page === 0}
                        onClick={() => setFiltros({ ...filtros, page: filtros.page - 1 })}
                        className="px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright disabled:opacity-40 text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all"
                    >
                        Anterior
                    </button>
                    <span className="text-caption text-on-surface-variant">
                        Página <strong className="text-on-surface">{filtros.page + 1}</strong> de{' '}
                        <strong className="text-on-surface">{pagina.totalPaginas}</strong>
                    </span>
                    <button
                        type="button"
                        disabled={filtros.page + 1 >= pagina.totalPaginas}
                        onClick={() => setFiltros({ ...filtros, page: filtros.page + 1 })}
                        className="px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright disabled:opacity-40 text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all"
                    >
                        Siguiente
                    </button>
                </div>
            )}
        </main>
    )
}
