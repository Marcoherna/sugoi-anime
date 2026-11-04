import { useEffect, useRef, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import { interaccionesApi } from '../api/interacciones'
import { usePerfil } from '../context/PerfilContext'
import { useProgreso } from '../hooks/useProgreso'
import Cargando from '../components/Cargando'

export default function Reproductor() {
    const { episodioId } = useParams()
    const navegar = useNavigate()
    const { perfilActivo } = usePerfil()
    const videoRef = useRef(null)

    const [datos, setDatos] = useState(null)
    const [listaEpisodios, setListaEpisodios] = useState([])
    const [segundoInicial, setSegundoInicial] = useState(0)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState(null)
    const [errorVideo, setErrorVideo] = useState(false)
    const [luzAmbiental, setLuzAmbiental] = useState(true)
    const [filtroEp, setFiltroEp] = useState('')

    const { alActualizarTiempo, alPausar, alTerminar } = useProgreso({
        perfilId: perfilActivo?.id,
        animeId: datos?.animeId,
        episodioId: Number(episodioId),
        numeroEpisodio: datos?.numeroEpisodio ?? datos?.numero,
        duracionSeg: datos?.duracionSeg ?? datos?.duracionSegundos,
    })

    useEffect(() => {
        let activo = true
        setCargando(true)
        setError(null)
        setErrorVideo(false)
        setSegundoInicial(0)

        async function cargar() {
            try {
                const reproduccion = await catalogoApi.reproduccion(episodioId)
                if (!activo) return
                setDatos(reproduccion)

                if (reproduccion?.animeId) {
                    catalogoApi
                        .episodios(reproduccion.animeId)
                        .then((eps) => {
                            if (activo) setListaEpisodios(eps || [])
                        })
                        .catch(() => {})
                }

                if (perfilActivo) {
                    const progreso = await interaccionesApi
                        .progresoEpisodio(perfilActivo.id, episodioId)
                        .catch(() => null)
                    const seg = progreso?.segundo ?? progreso?.segundoActual ?? 0
                    if (activo && seg > 0 && !progreso?.completado) {
                        setSegundoInicial(seg)
                    }
                }
            } catch (ex) {
                if (activo) setError(ex.message)
            } finally {
                if (activo) setCargando(false)
            }
        }

        cargar()
        return () => {
            activo = false
        }
    }, [episodioId, perfilActivo?.id])

    function alCargarMetadatos() {
        if (segundoInicial > 0 && videoRef.current) {
            videoRef.current.currentTime = segundoInicial
        }
    }

    if (cargando) return <Cargando texto="Preparando el reproductor..." />
    if (error || !datos) {
        return (
            <main className="pt-28 pb-12 max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop">
                <div className="p-6 rounded-xl bg-error-container/25 border border-error/40 text-error">
                    {error || 'No disponible'}
                </div>
            </main>
        )
    }

    const urlStream = datos.urlVideo || datos.streamUrl
    const numEp = datos.numeroEpisodio ?? datos.numero
    const tituloEp = datos.titulo || `Episodio ${numEp}`
    const tituloSerie = datos.tituloAnime || `Serie #${datos.animeId}`

    const indiceActual = listaEpisodios.findIndex((e) => Number(e.id) === Number(episodioId))
    const epAnterior = indiceActual > 0 ? listaEpisodios[indiceActual - 1] : null
    const epSiguiente =
        indiceActual >= 0 && indiceActual + 1 < listaEpisodios.length
            ? listaEpisodios[indiceActual + 1]
            : null

    const episodiosSidebar = listaEpisodios.filter((ep) => {
        const q = filtroEp.trim().toLowerCase()
        if (!q) return true
        return String(ep.numero).includes(q) || (ep.titulo || '').toLowerCase().includes(q)
    })

    return (
        <main className="flex-1 pt-24 pb-16 relative">
            {/* Ambient Stage Glow behind player */}
            {luzAmbiental && (
                <div
                    aria-hidden="true"
                    className="pointer-events-none fixed top-24 left-1/2 -translate-x-1/2 w-[90vw] max-w-[1200px] h-[500px] bg-primary-container/10 blur-[130px] rounded-full -z-10"
                />
            )}

            <div className="max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop">
                {/* Top Breadcrumb & Return Action Bar */}
                <div className="flex flex-wrap items-center justify-between gap-4 mb-5">
                    <div className="flex items-center gap-3">
                        <button
                            type="button"
                            onClick={() => navegar(`/anime/${datos.animeId}`)}
                            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface border border-outline-variant/25 transition-all duration-200 group shadow-sm"
                        >
                            <span className="material-symbols-outlined text-[18px] text-primary group-hover:-translate-x-0.5 transition-transform">
                                arrow_back
                            </span>
                            <span className="text-label-md font-medium">Volver a la ficha</span>
                        </button>
                        <div className="hidden sm:flex items-center gap-2 text-caption text-outline">
                            <span>/</span>
                            <Link
                                to={`/anime/${datos.animeId}`}
                                className="hover:text-on-surface transition-colors truncate max-w-[260px]"
                            >
                                {tituloSerie}
                            </Link>
                            <span>/</span>
                            <span className="text-primary font-medium">Episodio {numEp}</span>
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            onClick={() => setLuzAmbiental(!luzAmbiental)}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-caption transition-colors ${
                                luzAmbiental
                                    ? 'bg-primary/15 border-primary/40 text-primary'
                                    : 'bg-surface-container-low border-outline-variant/20 text-on-surface-variant hover:text-on-surface'
                            }`}
                        >
                            <span className="material-symbols-outlined text-[16px]">light_mode</span>
                            <span className="hidden md:inline">Luz ambiental</span>
                        </button>
                    </div>
                </div>

                {/* 12-Column Grid: Theater Video + Episode Sidebar */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
                    {/* Main Video Player Column (9 Cols on Desktop) */}
                    <div className="lg:col-span-9 flex flex-col gap-5">
                        {segundoInicial > 0 && (
                            <div className="flex items-center justify-between gap-3 px-4 py-2.5 rounded-xl bg-primary/15 border border-primary/30 text-sm text-on-surface">
                                <div className="flex items-center gap-2">
                                    <span className="material-symbols-outlined text-primary text-lg">
                                        history
                                    </span>
                                    <span>
                                        Reanudando desde el minuto{' '}
                                        <strong className="text-primary">
                                            {Math.floor(segundoInicial / 60)}:
                                            {String(Math.floor(segundoInicial % 60)).padStart(2, '0')}
                                        </strong>
                                    </span>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => {
                                        if (videoRef.current) videoRef.current.currentTime = 0
                                        setSegundoInicial(0)
                                    }}
                                    className="text-xs font-semibold text-primary hover:underline"
                                >
                                    Empezar desde 0:00
                                </button>
                            </div>
                        )}

                        {/* Video Player Frame */}
                        <div className="relative w-full aspect-video rounded-xl overflow-hidden bg-black shadow-[0_24px_60px_rgba(0,0,0,0.85)] ring-1 ring-outline-variant/30">
                            {!errorVideo ? (
                                <video
                                    ref={videoRef}
                                    className="w-full h-full object-contain bg-black"
                                    src={urlStream}
                                    controls
                                    autoPlay
                                    playsInline
                                    onLoadedMetadata={alCargarMetadatos}
                                    onTimeUpdate={alActualizarTiempo}
                                    onPause={alPausar}
                                    onEnded={alTerminar}
                                    onError={() => setErrorVideo(true)}
                                />
                            ) : (
                                <div className="w-full h-full flex flex-col items-center justify-center p-8 text-center bg-surface-container-low">
                                    <span className="material-symbols-outlined text-5xl text-error mb-3">
                                        videocam_off
                                    </span>
                                    <p className="font-title-card text-lg font-bold text-on-surface mb-2">
                                        El archivo de video no está disponible en el CDN en este momento
                                    </p>
                                    <p className="text-caption text-on-surface-variant max-w-lg mb-3">
                                        URL solicitada: <code className="text-primary">{urlStream}</code>
                                    </p>
                                </div>
                            )}
                        </div>

                        {/* Episode Information Deck (Underneath Player) */}
                        <div className="rounded-xl bg-surface-container-low border border-outline-variant/20 p-5 sm:p-6 shadow-lg flex flex-col gap-5">
                            <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                                <div className="space-y-1.5">
                                    <div className="flex flex-wrap items-center gap-2">
                                        <span className="px-2.5 py-0.5 rounded-md bg-primary-container/20 text-primary border border-primary/30 text-label-sm uppercase tracking-wider">
                                            Episodio {numEp}
                                        </span>
                                        <span className="px-2.5 py-0.5 rounded-md bg-surface-container-high text-on-surface-variant text-label-sm">
                                            SUB ESP
                                        </span>
                                        <span className="px-2.5 py-0.5 rounded-md bg-surface-container-high text-tertiary text-label-sm flex items-center gap-1">
                                            <span
                                                className="material-symbols-outlined text-[14px]"
                                                style={{ fontVariationSettings: "'FILL' 1" }}
                                            >
                                                bolt
                                            </span>
                                            Stream Activo
                                        </span>
                                    </div>
                                    <h1 className="font-headline-xl text-xl sm:text-2xl text-on-surface font-bold tracking-tight pt-1">
                                        {tituloSerie} — {tituloEp}
                                    </h1>
                                </div>

                                {/* Prev / Next Episode Controls */}
                                <div className="flex items-center gap-2.5 shrink-0">
                                    <button
                                        type="button"
                                        disabled={!epAnterior}
                                        onClick={() => epAnterior && navegar(`/ver/${epAnterior.id}`)}
                                        className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-lg bg-surface-container-high hover:bg-surface-bright disabled:opacity-40 text-on-surface text-label-md border border-outline-variant/30 transition-all"
                                    >
                                        <span className="material-symbols-outlined text-[18px]">
                                            skip_previous
                                        </span>
                                        <span className="hidden sm:inline">Anterior</span>
                                    </button>
                                    <button
                                        type="button"
                                        disabled={!epSiguiente}
                                        onClick={() => epSiguiente && navegar(`/ver/${epSiguiente.id}`)}
                                        className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-gradient-to-br from-primary to-primary-container disabled:opacity-40 text-on-primary font-semibold text-label-md shadow-[0_0_20px_rgba(160,120,255,0.25)] transition-all"
                                    >
                                        <span>Siguiente Episodio</span>
                                        <span className="material-symbols-outlined text-[18px]">
                                            skip_next
                                        </span>
                                    </button>
                                </div>
                            </div>

                            <div className="h-px bg-outline-variant/20" />

                            {/* Mandatory Legal & Attribution Metadata Bar (Ley 17.336) */}
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-1">
                                <div className="inline-flex items-center gap-2.5 px-3.5 py-2 rounded-lg bg-surface-container border border-outline-variant/25 w-fit">
                                    <span className="material-symbols-outlined text-[18px] text-secondary">
                                        verified_user
                                    </span>
                                    <span className="text-caption text-on-surface-variant">
                                        Licencia del contenido:{' '}
                                        <strong className="text-on-surface font-semibold">
                                            {datos.licencia || 'CC-BY-3.0'}
                                        </strong>
                                    </span>
                                    {datos.fuenteUrl && (
                                        <>
                                            <span className="text-outline-variant">·</span>
                                            <a
                                                href={datos.fuenteUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                                className="inline-flex items-center gap-1 text-caption font-medium text-secondary hover:text-primary transition-colors underline underline-offset-4"
                                            >
                                                <span>Fuente original</span>
                                                <span className="material-symbols-outlined text-[14px]">
                                                    open_in_new
                                                </span>
                                            </a>
                                        </>
                                    )}
                                </div>

                                <div className="flex items-center gap-4 text-caption text-outline">
                                    <span className="flex items-center gap-1.5">
                                        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                                        Progreso sincronizado automáticamente
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Right Column: Episode Playlist Sidebar (3 Cols on Desktop) */}
                    <aside className="lg:col-span-3 rounded-xl bg-surface-container-low border border-outline-variant/20 overflow-hidden shadow-lg flex flex-col max-h-[680px]">
                        <div className="p-4 bg-surface-container border-b border-outline-variant/20 flex items-center justify-between">
                            <div>
                                <h2 className="font-title-card text-base text-on-surface font-bold">
                                    Lista de Episodios
                                </h2>
                                <p className="text-caption text-outline">
                                    {listaEpisodios.length} episodios disponibles
                                </p>
                            </div>
                        </div>

                        <div className="p-3 border-b border-outline-variant/15 bg-surface-container-low">
                            <div className="relative">
                                <span className="material-symbols-outlined text-[18px] text-outline absolute left-3 top-1/2 -translate-y-1/2">
                                    search
                                </span>
                                <input
                                    type="text"
                                    value={filtroEp}
                                    onChange={(e) => setFiltroEp(e.target.value)}
                                    placeholder="Ir al episodio..."
                                    className="w-full pl-9 pr-3 py-1.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 text-caption text-on-surface placeholder:text-outline focus:outline-none focus:border-primary transition-colors"
                                />
                            </div>
                        </div>

                        <div className="overflow-y-auto divide-y divide-outline-variant/10 p-2 space-y-1 flex-1 custom-scrollbar">
                            {episodiosSidebar.map((ep) => {
                                const activo = Number(ep.id) === Number(episodioId)
                                return (
                                    <button
                                        key={ep.id}
                                        type="button"
                                        onClick={() => navegar(`/ver/${ep.id}`)}
                                        className={`w-full text-left flex items-center gap-3 p-2.5 rounded-lg transition-colors group ${
                                            activo
                                                ? 'bg-primary-container/15 border border-primary/40'
                                                : 'hover:bg-surface-container-high border border-transparent'
                                        }`}
                                    >
                                        <div
                                            className={`w-10 h-10 rounded flex items-center justify-center shrink-0 font-bold text-xs ${
                                                activo
                                                    ? 'bg-primary text-on-primary shadow-[0_0_10px_rgba(208,188,255,0.4)]'
                                                    : 'bg-surface-container-highest text-on-surface-variant group-hover:text-primary'
                                            }`}
                                        >
                                            {ep.numero}
                                        </div>
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-1.5">
                                                <span
                                                    className={`text-label-sm font-bold ${
                                                        activo ? 'text-primary' : 'text-outline'
                                                    }`}
                                                >
                                                    EP. {ep.numero}
                                                </span>
                                                {activo && (
                                                    <span className="text-[10px] px-1.5 py-0.2 rounded bg-primary/20 text-primary font-semibold">
                                                        Reproduciendo
                                                    </span>
                                                )}
                                            </div>
                                            <p
                                                className={`text-label-md font-medium truncate ${
                                                    activo
                                                        ? 'text-on-surface font-semibold'
                                                        : 'text-on-surface-variant group-hover:text-on-surface'
                                                }`}
                                            >
                                                {ep.titulo || `Episodio ${ep.numero}`}
                                            </p>
                                        </div>
                                    </button>
                                )
                            })}
                        </div>
                    </aside>
                </div>
            </div>
        </main>
    )
}
