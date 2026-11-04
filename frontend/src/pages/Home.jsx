import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usePerfil } from '../context/PerfilContext'
import { catalogoApi } from '../api/catalogo'
import { interaccionesApi } from '../api/interacciones'
import FilaAnimes from '../components/FilaAnimes'
import AnimeCard from '../components/AnimeCard'
import Filtros from '../components/Filtros'
import Cargando from '../components/Cargando'

export default function Home() {
    const { autenticado } = useAuth()
    const { perfilActivo } = usePerfil()
    const [recomendaciones, setRecomendaciones] = useState([])
    const [continuar, setContinuar] = useState([])
    const [populares, setPopulares] = useState([])
    const [cargando, setCargando] = useState(true)

    const [filtros, setFiltros] = useState({ page: 0, size: 12, sort: 'puntuacion,desc' })
    const [paginaCatalogo, setPaginaCatalogo] = useState(null)
    const [cargandoCatalogo, setCargandoCatalogo] = useState(false)

    useEffect(() => {
        let activo = true
        setCargando(true)

        const promesas = [catalogoApi.top(12)]
        if (perfilActivo) {
            promesas.push(
                interaccionesApi.continuarViendo(perfilActivo.id, 10),
                interaccionesApi.recomendaciones(perfilActivo.id)
            )
        }

        Promise.allSettled(promesas).then((resultados) => {
            if (!activo) return
            if (resultados[0]?.status === 'fulfilled') {
                setPopulares(resultados[0].value?.contenido || resultados[0].value?.content || [])
            }
            if (resultados[1]?.status === 'fulfilled') {
                setContinuar(resultados[1].value || [])
            } else {
                setContinuar([])
            }
            if (resultados[2]?.status === 'fulfilled') {
                setRecomendaciones(resultados[2].value || [])
            } else {
                setRecomendaciones([])
            }
            setCargando(false)
        })

        return () => {
            activo = false
        }
    }, [perfilActivo?.id])

    useEffect(() => {
        setCargandoCatalogo(true)
        catalogoApi
            .listar(filtros)
            .then(setPaginaCatalogo)
            .catch(() => setPaginaCatalogo({ contenido: [], totalPaginas: 0, totalElementos: 0 }))
            .finally(() => setCargandoCatalogo(false))
    }, [filtros.texto, filtros.genero, filtros.anio, filtros.sort, filtros.page])

    if (cargando) return <Cargando texto="Preparando tu inicio..." />

    const listaCatalogo = paginaCatalogo?.contenido || paginaCatalogo?.content || []

    return (
        <main className="pt-28 md:pt-24 pb-12 max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop w-full">
            {/* Ambient Background Glow */}
            <div className="fixed top-16 left-1/2 -translate-x-1/2 w-[800px] h-[320px] bg-primary/5 blur-[120px] rounded-full pointer-events-none -z-10" />

            {!autenticado && (
                <section className="mb-8 rounded-xl bg-gradient-to-r from-surface-container-low via-surface-container to-surface-container-low p-5 border border-outline-variant/25 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-lg">
                    <div className="flex items-center gap-3.5">
                        <div className="w-10 h-10 rounded-xl bg-primary/15 border border-primary/30 flex items-center justify-center text-primary shrink-0">
                            <span className="material-symbols-outlined">bolt</span>
                        </div>
                        <p className="text-body-md text-sm text-on-surface-variant">
                            <strong className="text-on-surface">Bienvenido a SugoiAnime.</strong> Puedes explorar el catálogo libremente o{' '}
                            <Link to="/registro" className="text-primary font-semibold hover:underline">
                                crear una cuenta
                            </Link>{' '}
                            para reproducir capítulos, guardar favoritos y recibir recomendaciones personalizadas.
                        </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                        <Link
                            to="/login"
                            className="px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all"
                        >
                            Iniciar sesión
                        </Link>
                    </div>
                </section>
            )}

            {/* Personalization Section: Recomendados para ti */}
            {perfilActivo && recomendaciones.length === 0 ? (
                <section aria-label="Recomendaciones personalizadas" className="mb-10">
                    <div className="relative overflow-hidden rounded-xl bg-gradient-to-r from-surface-container-low via-surface-container to-surface-container-low p-5 md:p-6 border border-outline-variant/25 shadow-xl">
                        <div className="absolute -right-12 -top-12 w-48 h-48 bg-primary/10 rounded-full blur-2xl pointer-events-none" />
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 relative z-10">
                            <div className="flex items-start sm:items-center gap-4">
                                <div className="w-11 h-11 rounded-xl bg-primary/15 border border-primary/30 flex items-center justify-center shrink-0 text-primary">
                                    <span className="material-symbols-outlined text-2xl">auto_awesome</span>
                                </div>
                                <div>
                                    <div className="flex items-center gap-2">
                                        <h2 className="font-headline-xl text-lg md:text-xl font-bold text-on-surface">
                                            Recomendados para {perfilActivo?.nombre}
                                        </h2>
                                        <span className="px-2 py-0.5 rounded-full bg-primary/15 text-primary text-label-sm">
                                            IA Motor Activo
                                        </span>
                                    </div>
                                    <p className="text-body-md text-sm text-on-surface-variant mt-1">
                                        Marca al menos dos animes como favoritos para recibir recomendaciones personalizadas con similitud de coseno (TF-IDF).
                                    </p>
                                </div>
                            </div>
                            <Link
                                to="/catalogo"
                                className="self-start sm:self-center shrink-0 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-primary text-label-md border border-primary/20 transition-all hover:shadow-[0_0_15px_rgba(208,188,255,0.2)]"
                            >
                                <span className="material-symbols-outlined text-lg">favorite</span>
                                <span>Explorar para agregar</span>
                            </Link>
                        </div>
                    </div>
                </section>
            ) : (
                recomendaciones.length > 0 && (
                    <FilaAnimes
                        titulo={`Recomendados para ${perfilActivo?.nombre || 'ti'}`}
                        subtitulo="Según tus gustos (Similitud de Coseno TF-IDF)"
                        icono="auto_awesome"
                        colorIcono="text-primary"
                        animes={recomendaciones}
                    />
                )
            )}

            {/* Continuar viendo */}
            {continuar.length > 0 && (
                <FilaAnimes
                    titulo="Continuar viendo"
                    subtitulo="Retoma donde lo dejaste"
                    icono="play_circle"
                    colorIcono="text-secondary"
                    animes={continuar}
                />
            )}

            {/* Mejor valorados */}
            <FilaAnimes
                titulo="Mejor valorados"
                subtitulo="Los favoritos de la comunidad"
                icono="workspace_premium"
                colorIcono="text-tertiary"
                animes={populares}
            />

            {/* Catálogo Rápido Integrado (Stitch Design: Catálogo e Inicio) */}
            <div className="mt-14 pt-8 border-t border-outline-variant/20">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-2 mb-6">
                    <div>
                        <span className="text-label-sm uppercase tracking-widest text-primary block mb-1">
                            Directorio Completo
                        </span>
                        <h2 className="font-headline-xl text-2xl md:text-3xl font-extrabold text-on-surface tracking-tight">
                            Catálogo de Anime
                        </h2>
                    </div>
                    <p className="text-caption text-on-surface-variant">
                        Mostrando{' '}
                        <span className="text-on-surface font-semibold">
                            {paginaCatalogo?.totalElementos ?? listaCatalogo.length}
                        </span>{' '}
                        series disponibles en español
                    </p>
                </div>

                <Filtros valores={filtros} onCambio={setFiltros} />

                {cargandoCatalogo ? (
                    <Cargando texto="Buscando animes..." />
                ) : listaCatalogo.length === 0 ? (
                    <div className="bg-surface-container-low border border-outline-variant/25 rounded-2xl p-10 text-center">
                        <span className="material-symbols-outlined text-4xl text-outline mb-2">search_off</span>
                        <p className="text-on-surface font-semibold">No se encontraron animes con esos filtros.</p>
                    </div>
                ) : (
                    <>
                        <section
                            aria-label="Resultados del catálogo"
                            className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-gutter lg:gap-gutter-desktop"
                        >
                            {listaCatalogo.map((anime) => (
                                <AnimeCard key={anime.id} anime={anime} />
                            ))}
                        </section>

                        {paginaCatalogo && paginaCatalogo.totalPaginas > 1 && (
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
                                    <strong className="text-on-surface">{paginaCatalogo.totalPaginas}</strong>
                                </span>
                                <button
                                    type="button"
                                    disabled={filtros.page + 1 >= paginaCatalogo.totalPaginas}
                                    onClick={() => setFiltros({ ...filtros, page: filtros.page + 1 })}
                                    className="px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright disabled:opacity-40 text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all"
                                >
                                    Siguiente
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </main>
    )
}
