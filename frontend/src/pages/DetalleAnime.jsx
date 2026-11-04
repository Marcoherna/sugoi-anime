import { useEffect, useState, useMemo } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import { interaccionesApi } from '../api/interacciones'
import { useAuth } from '../context/AuthContext'
import { usePerfil } from '../context/PerfilContext'
import Cargando from '../components/Cargando'

export default function DetalleAnime() {
    const { id } = useParams()
    const navegar = useNavigate()
    const { autenticado } = useAuth()
    const { perfilActivo } = usePerfil()

    const [anime, setAnime] = useState(null)
    const [episodios, setEpisodios] = useState([])
    const [esFavorito, setEsFavorito] = useState(false)
    const [progresos, setProgresos] = useState({})
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState(null)

    const [filtroEp, setFiltroEp] = useState('')
    const [ordenAsc, setOrdenAsc] = useState(true)

    useEffect(() => {
        let activo = true
        setCargando(true)
        Promise.allSettled([catalogoApi.detalle(id), catalogoApi.episodios(id)])
            .then(([detRes, epsRes]) => {
                if (!activo) return
                if (detRes.status === 'rejected') {
                    setError(detRes.reason?.message || 'Anime no encontrado')
                    return
                }
                const datosAnime = detRes.value
                setAnime(datosAnime)
                const listaEps =
                    (epsRes.status === 'fulfilled' && epsRes.value) ||
                    datosAnime?.episodiosDisponibles ||
                    []
                setEpisodios(listaEps)
            })
            .finally(() => {
                if (activo) setCargando(false)
            })
        return () => {
            activo = false
        }
    }, [id])

    useEffect(() => {
        if (!perfilActivo) return
        let activo = true

        interaccionesApi
            .estadoFavorito(perfilActivo.id, id)
            .then((r) => {
                if (activo) setEsFavorito(Boolean(r?.esFavorito))
            })
            .catch(() => {})

        interaccionesApi
            .progresoAnime(perfilActivo.id, id)
            .then((lista) => {
                if (!activo) return
                const mapa = {}
                ;(lista || []).forEach((p) => {
                    mapa[p.episodioId] = p
                })
                setProgresos(mapa)
            })
            .catch(() => {})

        return () => {
            activo = false
        }
    }, [id, perfilActivo?.id])

    async function alternarFavorito() {
        if (!autenticado) {
            navegar('/login')
            return
        }
        if (!perfilActivo) {
            navegar('/perfiles')
            return
        }
        try {
            if (esFavorito) {
                await interaccionesApi.quitarFavorito(perfilActivo.id, id)
                setEsFavorito(false)
            } else {
                await interaccionesApi.agregarFavorito(perfilActivo.id, id)
                setEsFavorito(true)
            }
            interaccionesApi.regenerarRecomendaciones(perfilActivo.id).catch(() => {})
        } catch (ex) {
            setError(ex.message)
        }
    }

    function formatearDuracion(segundos) {
        if (!segundos) return '24 min'
        const min = Math.floor(segundos / 60)
        return `${min} min`
    }

    const episodiosFiltrados = useMemo(() => {
        const q = filtroEp.trim().toLowerCase()
        const filtrados = episodios.filter((ep) => {
            if (!q) return true
            return String(ep.numero).includes(q) || (ep.titulo || '').toLowerCase().includes(q)
        })
        return [...filtrados].sort((a, b) => (ordenAsc ? a.numero - b.numero : b.numero - a.numero))
    }, [episodios, filtroEp, ordenAsc])

    if (cargando) return <Cargando />
    if (error || !anime) {
        return (
            <main className="pt-28 pb-12 max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop">
                <div className="p-6 rounded-xl bg-error-container/25 border border-error/40 text-error">
                    {error || 'Anime no encontrado'}
                </div>
            </main>
        )
    }

    const primerEpisodio = episodios.length > 0 ? episodios[0] : null
    const subtituloJapones = anime.tituloJapones || anime.tituloOriginal

    return (
        <main className="relative pt-24 pb-20 overflow-hidden">
            {/* Ambient Stage Lighting Background */}
            <div className="pointer-events-none absolute top-0 inset-x-0 h-[620px] -z-10 overflow-hidden">
                {anime.imagenUrl && (
                    <img
                        src={anime.imagenUrl}
                        alt=""
                        aria-hidden="true"
                        className="w-full h-full object-cover object-center opacity-15 blur-3xl scale-110"
                    />
                )}
                <div className="absolute inset-0 bg-gradient-to-b from-surface-container-lowest/40 via-surface-container-lowest/85 to-surface-container-lowest" />
                <div className="absolute top-16 left-1/4 w-96 h-96 bg-primary-container/15 rounded-full blur-[120px]" />
            </div>

            <div className="max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop">
                {/* Breadcrumb / Back Navigation */}
                <div className="py-4 flex items-center gap-2 text-caption text-on-surface-variant">
                    <Link to="/catalogo" className="hover:text-primary transition-colors flex items-center gap-1">
                        <span className="material-symbols-outlined text-base">arrow_back</span>
                        Catálogo
                    </Link>
                    <span className="text-outline">/</span>
                    <span className="text-on-surface font-medium truncate">{anime.titulo}</span>
                </div>

                {/* HERO SECTION: 12-Column Asymmetric Layout */}
                <section className="mt-2 rounded-2xl bg-surface-container-low/90 backdrop-blur-md p-6 md:p-8 lg:p-10 shadow-[0_24px_48px_rgba(0,0,0,0.6)] border border-outline-variant/20 relative overflow-hidden">
                    <div className="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent" />

                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-start">
                        {/* Left Column: Poster Artwork (3 Cols on Desktop) */}
                        <div className="lg:col-span-3 sm:max-w-[280px] lg:max-w-none mx-auto w-full">
                            <div className="relative group aspect-[2/3] rounded-xl overflow-hidden bg-surface-container-high shadow-[0_16px_32px_rgba(0,0,0,0.5)] border border-outline-variant/30">
                                {anime.imagenUrl ? (
                                    <img
                                        src={anime.imagenUrl}
                                        alt={`Portada de ${anime.titulo}`}
                                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                                    />
                                ) : (
                                    <div className="w-full h-full flex items-center justify-center text-outline">
                                        Sin portada
                                    </div>
                                )}
                                <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/80 via-transparent to-transparent opacity-60" />

                                {anime.puntuacion && (
                                    <div className="absolute top-3 left-3 flex items-center gap-1 px-2.5 py-1 rounded-md bg-surface-container-lowest/85 backdrop-blur-md border border-tertiary/30 text-tertiary text-label-sm shadow-lg">
                                        <span className="material-symbols-outlined text-sm icon-filled">star</span>
                                        <span>{Number(anime.puntuacion).toFixed(1)}</span>
                                    </div>
                                )}

                                <div className="absolute top-3 right-3 px-2 py-0.5 rounded bg-primary-container/90 backdrop-blur-md text-on-primary text-[11px] font-bold tracking-wider uppercase">
                                    HD 1080p
                                </div>
                            </div>

                            <div className="mt-4 grid grid-cols-2 gap-2 text-center">
                                <div className="bg-surface-container rounded-lg p-2.5 border border-outline-variant/15">
                                    <span className="block text-[11px] text-outline uppercase tracking-wider font-semibold">
                                        Estudio
                                    </span>
                                    <span className="text-label-md text-on-surface font-semibold mt-0.5 block truncate">
                                        {anime.estudio || 'Oficial'}
                                    </span>
                                </div>
                                <div className="bg-surface-container rounded-lg p-2.5 border border-outline-variant/15">
                                    <span className="block text-[11px] text-outline uppercase tracking-wider font-semibold">
                                        Formato
                                    </span>
                                    <span className="text-label-md text-on-surface font-semibold mt-0.5 block">
                                        TV Serie
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Right Column: Metadata, Synopsis, Actions (9 Cols on Desktop) */}
                        <div className="lg:col-span-9 flex flex-col justify-between h-full space-y-6">
                            <div>
                                <div className="space-y-1.5">
                                    {subtituloJapones && (
                                        <span className="inline-block text-label-sm uppercase tracking-widest text-primary font-semibold">
                                            {subtituloJapones}
                                        </span>
                                    )}
                                    <h1 className="font-display-hero text-3xl sm:text-4xl lg:text-[2.75rem] font-extrabold text-on-surface tracking-tight leading-[1.15]">
                                        {anime.titulo}
                                    </h1>
                                </div>

                                {/* Metadata Ribbon */}
                                <div className="mt-4 flex flex-wrap items-center gap-3 text-label-md">
                                    {anime.puntuacion && (
                                        <>
                                            <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-tertiary/15 border border-tertiary/30 text-tertiary font-semibold">
                                                <span className="material-symbols-outlined text-base icon-filled">star</span>
                                                <span>★ {Number(anime.puntuacion).toFixed(2)}</span>
                                            </div>
                                            <span className="text-outline-variant">•</span>
                                        </>
                                    )}
                                    {anime.anio && (
                                        <>
                                            <div className="flex items-center gap-1.5 text-on-surface">
                                                <span className="material-symbols-outlined text-base text-outline">
                                                    calendar_today
                                                </span>
                                                <span>{anime.anio}</span>
                                            </div>
                                            <span className="text-outline-variant">•</span>
                                        </>
                                    )}
                                    <div className="flex items-center gap-1.5 text-on-surface">
                                        <span className="material-symbols-outlined text-base text-outline">
                                            video_library
                                        </span>
                                        <span>
                                            {anime.episodios || anime.numeroEpisodios || episodios.length} episodios
                                        </span>
                                    </div>
                                    <span className="text-outline-variant">•</span>
                                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-label-sm font-semibold">
                                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                                        {anime.estado || 'Finalizado'}
                                    </span>
                                </div>

                                {/* Genre Tags */}
                                {anime.generos?.length > 0 && (
                                    <div className="mt-5 flex flex-wrap gap-2">
                                        {anime.generos.map((g) => (
                                            <span
                                                key={g.id ?? g}
                                                className="px-3.5 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-secondary text-label-sm font-medium border border-outline-variant/30 transition-colors"
                                            >
                                                {g.nombre ?? g}
                                            </span>
                                        ))}
                                    </div>
                                )}

                                {/* Synopsis */}
                                <div className="mt-6 space-y-3 max-w-[68ch]">
                                    <h2 className="text-label-sm uppercase tracking-wider text-outline font-semibold">
                                        Sinopsis
                                    </h2>
                                    <p className="text-body-md text-on-surface/90 leading-relaxed">
                                        {anime.sinopsis || 'Sin sinopsis disponible.'}
                                    </p>

                                    {anime.sinopsisTraducida && (
                                        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-surface-container border border-outline-variant/25 text-caption text-outline">
                                            <span className="material-symbols-outlined text-sm text-primary">
                                                translate
                                            </span>
                                            <span>Sinopsis traducida automáticamente al español</span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Action Buttons */}
                            <div className="pt-4 border-t border-outline-variant/20 flex flex-wrap items-center gap-4">
                                {autenticado ? (
                                    <button
                                        type="button"
                                        onClick={alternarFavorito}
                                        className={`group relative inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl font-bold text-label-md transition-all duration-200 active:scale-[0.98] ${
                                            esFavorito
                                                ? 'bg-surface-container-high text-primary border border-primary/40 shadow-[0_0_20px_rgba(208,188,255,0.2)]'
                                                : 'bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_0_24px_rgba(160,120,255,0.35)] hover:shadow-[0_0_32px_rgba(160,120,255,0.55)]'
                                        }`}
                                    >
                                        <span
                                            className={`material-symbols-outlined text-xl transition-transform group-hover:scale-110 ${
                                                esFavorito ? 'icon-filled text-primary' : ''
                                            }`}
                                        >
                                            favorite
                                        </span>
                                        <span>{esFavorito ? '♥ En favoritos' : '♡ Agregar a favoritos'}</span>
                                    </button>
                                ) : (
                                    <Link
                                        to="/login"
                                        className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_0_24px_rgba(160,120,255,0.35)]"
                                    >
                                        <span className="material-symbols-outlined text-xl">favorite</span>
                                        <span>Inicia sesión para guardar</span>
                                    </Link>
                                )}

                                {primerEpisodio && (
                                    <button
                                        type="button"
                                        onClick={() => navegar(`/ver/${primerEpisodio.id}`)}
                                        className="inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-xl bg-surface-container-high hover:bg-surface-bright text-on-surface font-semibold text-label-md border border-outline-variant/30 transition-all duration-200"
                                    >
                                        <span className="material-symbols-outlined text-xl text-primary icon-filled">
                                            play_arrow
                                        </span>
                                        <span>Comenzar a ver (Ep. {primerEpisodio.numero})</span>
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                </section>

                {/* EPISODES SECTION */}
                <section className="mt-12 space-y-6">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-outline-variant/20">
                        <div className="flex items-center gap-3">
                            <div className="w-1.5 h-7 rounded-full bg-gradient-to-b from-primary to-primary-container" />
                            <h2 className="font-headline-xl text-2xl font-bold text-on-surface">
                                Episodios disponibles
                            </h2>
                            <span className="px-2.5 py-0.5 rounded-full bg-surface-container-high text-on-surface-variant text-label-sm">
                                {episodios.length} disponibles
                            </span>
                        </div>

                        {episodios.length > 0 && (
                            <div className="flex items-center gap-3">
                                <div className="relative">
                                    <span className="material-symbols-outlined text-lg text-outline absolute left-3 top-1/2 -translate-y-1/2">
                                        search
                                    </span>
                                    <input
                                        type="text"
                                        value={filtroEp}
                                        onChange={(e) => setFiltroEp(e.target.value)}
                                        placeholder="Buscar episodio..."
                                        className="pl-9 pr-4 py-2 rounded-lg bg-surface-container-low border border-outline-variant/30 text-label-md text-on-surface placeholder:text-outline focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary w-full sm:w-56 transition-all"
                                    />
                                </div>
                                <button
                                    type="button"
                                    onClick={() => setOrdenAsc(!ordenAsc)}
                                    title="Cambiar orden"
                                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-surface-container-low hover:bg-surface-container-high border border-outline-variant/30 text-label-md text-on-surface-variant hover:text-on-surface transition-colors"
                                >
                                    <span className="material-symbols-outlined text-lg">swap_vert</span>
                                    <span className="hidden sm:inline">
                                        {ordenAsc ? 'Orden: 1 - N' : 'Orden: N - 1'}
                                    </span>
                                </button>
                            </div>
                        )}
                    </div>

                    {episodiosFiltrados.length === 0 ? (
                        <div className="p-8 rounded-xl bg-surface-container-low border border-outline-variant/20 text-center text-on-surface-variant">
                            Aún no hay episodios disponibles para este título.
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {episodiosFiltrados.map((ep) => {
                                const prog = progresos[ep.id]
                                return (
                                    <article
                                        key={ep.id}
                                        onClick={() => navegar(`/ver/${ep.id}`)}
                                        className="group relative rounded-xl bg-surface-container-low hover:bg-surface-container-high p-4 sm:p-5 border border-outline-variant/15 hover:border-primary/40 transition-all duration-200 shadow-sm hover:shadow-[0_8px_24px_rgba(0,0,0,0.4)] cursor-pointer overflow-hidden"
                                    >
                                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                                            <div className="flex items-start sm:items-center gap-4 min-w-0">
                                                <div className="relative flex-shrink-0 w-16 h-16 sm:w-24 sm:h-16 rounded-lg bg-surface-container-highest overflow-hidden flex items-center justify-center border border-outline-variant/30 group-hover:border-primary/50 transition-colors">
                                                    {anime.imagenUrl && (
                                                        <img
                                                            src={anime.imagenUrl}
                                                            alt=""
                                                            className="absolute inset-0 w-full h-full object-cover opacity-30 group-hover:scale-110 transition-transform duration-500"
                                                        />
                                                    )}
                                                    <div className="absolute inset-0 bg-surface-container-lowest/50 group-hover:bg-primary-container/20 transition-colors" />
                                                    <span className="relative z-10 font-headline-xl text-base font-extrabold text-primary group-hover:scale-105 transition-transform">
                                                        Ep. {ep.numero}
                                                    </span>
                                                </div>

                                                <div className="space-y-1 min-w-0">
                                                    <div className="flex items-center gap-2 flex-wrap">
                                                        <span className="text-label-sm uppercase tracking-wider text-primary font-bold">
                                                            Episodio {ep.numero}
                                                        </span>
                                                        <span className="text-outline-variant">•</span>
                                                        <span className="inline-flex items-center gap-1 text-caption text-on-surface-variant">
                                                            <span className="material-symbols-outlined text-[14px]">
                                                                schedule
                                                            </span>
                                                            {formatearDuracion(ep.duracionSegundos || ep.duracionSeg)}
                                                        </span>
                                                        {prog && (
                                                            <>
                                                                <span className="text-outline-variant">•</span>
                                                                <span className="px-2 py-0.5 rounded bg-primary/15 text-primary text-[11px] font-semibold">
                                                                    {prog.completado
                                                                        ? '✓ Visto'
                                                                        : `${prog.porcentaje}% visto`}
                                                                </span>
                                                            </>
                                                        )}
                                                    </div>

                                                    <h3 className="font-title-card text-base sm:text-lg font-semibold text-on-surface group-hover:text-primary transition-colors truncate">
                                                        {ep.titulo || `Episodio ${ep.numero}`}
                                                    </h3>

                                                    <div className="flex flex-wrap items-center gap-2 pt-0.5">
                                                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-surface-container text-[11px] font-medium text-outline border border-outline-variant/25">
                                                            <span className="material-symbols-outlined text-[13px] text-secondary">
                                                                verified_user
                                                            </span>
                                                            Licencia: {ep.licencia || 'CC-BY-3.0'}
                                                        </span>
                                                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-surface-container text-[11px] font-medium text-outline border border-outline-variant/25">
                                                            SUB ESP
                                                        </span>
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="flex items-center justify-end sm:flex-shrink-0">
                                                <button
                                                    type="button"
                                                    onClick={(e) => {
                                                        e.stopPropagation()
                                                        navegar(`/ver/${ep.id}`)
                                                    }}
                                                    className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_4px_16px_rgba(160,120,255,0.25)] group-hover:shadow-[0_6px_22px_rgba(160,120,255,0.45)] transition-all active:scale-95"
                                                >
                                                    <span className="material-symbols-outlined text-lg icon-filled">
                                                        play_arrow
                                                    </span>
                                                    <span>{prog && !prog.completado ? 'Continuar' : 'Reproducir'}</span>
                                                </button>
                                            </div>
                                        </div>

                                        {prog && prog.porcentaje > 0 && (
                                            <div className="absolute bottom-0 inset-x-0 h-1 bg-surface-container-highest">
                                                <div
                                                    className="h-full bg-gradient-to-r from-primary to-primary-container"
                                                    style={{ width: `${prog.porcentaje}%` }}
                                                />
                                            </div>
                                        )}
                                    </article>
                                )
                            })}
                        </div>
                    )}
                </section>
            </div>
        </main>
    )
}
