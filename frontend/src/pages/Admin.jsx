import { useEffect, useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { catalogoApi } from '../api/catalogo'
import { adminApi } from '../api/admin'
import Cargando from '../components/Cargando'

export default function Admin() {
    const { esAdmin } = useAuth()
    const navegar = useNavigate()

    const [tab, setTab] = useState('series') // 'series' | 'capitulos' | 'cuentas'
    const [cargando, setCargando] = useState(true)
    const [mensaje, setMensaje] = useState(null)
    const [error, setError] = useState(null)

    // --- Estado Series ---
    const [animes, setAnimes] = useState([])
    const [generosCatalogo, setGenerosCatalogo] = useState([])
    const [editandoAnimeId, setEditandoAnimeId] = useState(null)
    const [mostrarFormManual, setMostrarFormManual] = useState(false)
    const [nombreBusquedaJikan, setNombreBusquedaJikan] = useState('')
    const [importandoJikan, setImportandoJikan] = useState(false)
    const [resultadosJikan, setResultadosJikan] = useState([])
    const [buscandoJikan, setBuscandoJikan] = useState(false)
    const [filtroSeries, setFiltroSeries] = useState('')
    const [ordenSeriesScore, setOrdenSeriesScore] = useState(true)
    const [formAnime, setFormAnime] = useState({
        titulo: '',
        tituloJapones: '',
        sinopsis: '',
        imagenKey: '',
        anio: 2024,
        estado: 'EN_EMISION',
        episodios: 12,
        puntuacion: 8.0,
        generoIds: [],
    })

    // --- Estado Capítulos y S3 ---
    const [animeSeleccionadoId, setAnimeSeleccionadoId] = useState('')
    const [episodios, setEpisodios] = useState([])
    const [videosS3, setVideosS3] = useState([])
    const [archivoVideo, setArchivoVideo] = useState(null)
    const [subiendoVideo, setSubiendoVideo] = useState(false)
    const [autocompletandoCapitulo, setAutocompletandoCapitulo] = useState(false)
    const [capituloAutocompletado, setCapituloAutocompletado] = useState(false)
    const [totalEpisodiosSugeridos, setTotalEpisodiosSugeridos] = useState(24)
    const [formEpisodio, setFormEpisodio] = useState({
        numero: 1,
        titulo: '',
        duracionSeg: 1440,
        videoKey: '',
        licencia: 'CC-BY-3.0',
        fuenteUrl: '',
    })

    // --- Estado Cuentas ---
    const [usuarios, setUsuarios] = useState([])
    const [busquedaUsuario, setBusquedaUsuario] = useState('')
    const [filtroRol, setFiltroRol] = useState('')
    const [filtroPlan, setFiltroPlan] = useState('')

    useEffect(() => {
        if (!esAdmin) {
            navegar('/')
            return
        }
        cargarTodo()
    }, [esAdmin])

    async function cargarTodo() {
        setCargando(true)
        setError(null)
        try {
            const [paginaAnimes, listaGeneros, listaUsuarios, listaS3] = await Promise.all([
                catalogoApi.listar({ page: 0, size: 100, sort: 'id,desc' }),
                catalogoApi.generos().catch(() => []),
                adminApi.listarUsuarios().catch(() => []),
                adminApi.listarVideosS3().catch(() => []),
            ])
            const listaA = paginaAnimes?.contenido || paginaAnimes?.content || []
            setAnimes(listaA)
            setGenerosCatalogo(listaGeneros || [])
            if (listaA.length > 0 && !animeSeleccionadoId) {
                setAnimeSeleccionadoId(String(listaA[0].id))
            }
            setUsuarios(listaUsuarios || [])
            setVideosS3(listaS3 || [])
        } catch (e) {
            setError(e.message || 'Error al cargar datos de administración')
        } finally {
            setCargando(false)
        }
    }

    useEffect(() => {
        if (animeSeleccionadoId) {
            catalogoApi
                .episodios(animeSeleccionadoId)
                .then((eps) => {
                    const lista = eps || []
                    setEpisodios(lista)
                    const maxNum = lista.reduce((acc, item) => Math.max(acc, item.numero || 0), 0)
                    cambiarNumeroCapitulo(maxNum + 1, animeSeleccionadoId)
                })
                .catch(() => setEpisodios([]))
        }
    }, [animeSeleccionadoId])

    async function cambiarNumeroCapitulo(nuevoNumero, idAnimeOverride) {
        const num = Number(nuevoNumero)
        setFormEpisodio((prev) => ({ ...prev, numero: num }))
        const targetAnimeId = idAnimeOverride || animeSeleccionadoId
        if (!targetAnimeId || !num || num < 1) return

        setAutocompletandoCapitulo(true)
        setCapituloAutocompletado(false)
        try {
            const sugerencia = await adminApi.sugerirEpisodio(targetAnimeId, num)
            if (sugerencia) {
                if (sugerencia.totalEpisodiosSerie) {
                    setTotalEpisodiosSugeridos(sugerencia.totalEpisodiosSerie)
                }
                setFormEpisodio((prev) => ({
                    ...prev,
                    numero: num,
                    titulo: sugerencia.titulo || `Episodio ${num}`,
                    duracionSeg: sugerencia.duracionSeg ?? sugerencia.duracionSegundos ?? 1440,
                    licencia: sugerencia.licencia || 'CC-BY-3.0',
                    fuenteUrl: sugerencia.fuenteUrl || '',
                    videoKey: prev.videoKey || sugerencia.videoKey || '',
                }))
                setCapituloAutocompletado(true)
            }
        } catch {
            // Permite llenado manual si falla la consulta externa
        } finally {
            setAutocompletandoCapitulo(false)
        }
    }

    function mostrarAviso(texto) {
        setMensaje(texto)
        setError(null)
        setTimeout(() => setMensaje(null), 5000)
    }

    // =========================================================================
    // Acciones de Series (Animes)
    // =========================================================================
    async function importarDirectoPorNombre(e) {
        e.preventDefault()
        if (!nombreBusquedaJikan.trim()) return
        setImportandoJikan(true)
        setError(null)
        try {
            const creado = await adminApi.importarPorNombre(nombreBusquedaJikan.trim())
            mostrarAviso(
                `Serie "${creado.titulo}" importada automáticamente desde la API con todos sus datos.`
            )
            setNombreBusquedaJikan('')
            setResultadosJikan([])
            await cargarTodo()
            setAnimeSeleccionadoId(String(creado.id))
        } catch (err) {
            setError(err.message || 'No se pudo importar la serie desde la API')
        } finally {
            setImportandoJikan(false)
        }
    }

    async function buscarOpcionesEnJikan() {
        if (!nombreBusquedaJikan.trim()) return
        setBuscandoJikan(true)
        setError(null)
        try {
            const lista = await adminApi.buscarEnJikan(nombreBusquedaJikan.trim())
            setResultadosJikan(lista || [])
            if (!lista || lista.length === 0) {
                setError('No se encontraron coincidencias para ese nombre')
            }
        } catch (err) {
            setError(err.message || 'Error al buscar opciones')
        } finally {
            setBuscandoJikan(false)
        }
    }

    async function importarCandidatoJikan(candidato) {
        setImportandoJikan(true)
        setError(null)
        try {
            const malId = candidato.mal_id ?? candidato.malId
            const creado = malId
                ? await adminApi.importarPorMalId(malId)
                : await adminApi.importarPorNombre(candidato.title || candidato.titulo)
            mostrarAviso(`Serie "${creado.titulo}" agregada exitosamente al catálogo.`)
            setNombreBusquedaJikan('')
            setResultadosJikan([])
            await cargarTodo()
            setAnimeSeleccionadoId(String(creado.id))
        } catch (err) {
            setError(err.message || 'Error al agregar la serie seleccionada')
        } finally {
            setImportandoJikan(false)
        }
    }

    async function guardarAnime(e) {
        e.preventDefault()
        setError(null)
        const payload = {
            titulo: formAnime.titulo,
            tituloJapones: formAnime.tituloJapones,
            sinopsis: formAnime.sinopsis,
            imagenKey: formAnime.imagenKey,
            anio: Number(formAnime.anio),
            estado: formAnime.estado,
            episodios: Number(formAnime.episodios),
            puntuacion: Number(formAnime.puntuacion),
            generoIds: formAnime.generoIds,
        }

        try {
            if (editandoAnimeId) {
                await adminApi.actualizarAnime(editandoAnimeId, payload)
                mostrarAviso('Serie actualizada correctamente.')
            } else {
                const creado = await adminApi.crearAnime(payload)
                mostrarAviso(`Serie "${creado.titulo}" creada correctamente.`)
                setAnimeSeleccionadoId(String(creado.id))
            }
            limpiarFormAnime()
            await cargarTodo()
        } catch (err) {
            setError(err.message || 'No se pudo guardar la serie')
        }
    }

    async function seleccionarParaEditar(anime) {
        setEditandoAnimeId(anime.id)
        setMostrarFormManual(true)
        try {
            const det = await catalogoApi.detalle(anime.id)
            setFormAnime({
                titulo: det.titulo || anime.titulo || '',
                tituloJapones: det.tituloJapones || '',
                sinopsis: det.sinopsis || '',
                imagenKey: det.imagenUrl || anime.imagenUrl || '',
                anio: det.anio || anime.anio || 2024,
                estado: det.estado || 'FINALIZADO',
                episodios: det.episodios || 12,
                puntuacion: det.puntuacion || anime.puntuacion || 8.0,
                generoIds: (det.generos || anime.generos || []).map((g) => g.id).filter(Boolean),
            })
        } catch {
            setFormAnime({
                titulo: anime.titulo || '',
                tituloJapones: '',
                sinopsis: '',
                imagenKey: anime.imagenUrl || '',
                anio: anime.anio || 2024,
                estado: 'FINALIZADO',
                episodios: 12,
                puntuacion: anime.puntuacion || 8.0,
                generoIds: (anime.generos || []).map((g) => g.id).filter(Boolean),
            })
        }
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    function limpiarFormAnime() {
        setEditandoAnimeId(null)
        setMostrarFormManual(false)
        setFormAnime({
            titulo: '',
            tituloJapones: '',
            sinopsis: '',
            imagenKey: '',
            anio: 2024,
            estado: 'EN_EMISION',
            episodios: 12,
            puntuacion: 8.0,
            generoIds: [],
        })
    }

    function toggleGeneroId(id) {
        setFormAnime((prev) => {
            const existe = prev.generoIds.includes(id)
            return {
                ...prev,
                generoIds: existe
                    ? prev.generoIds.filter((g) => g !== id)
                    : [...prev.generoIds, id],
            }
        })
    }

    async function borrarAnime(id, titulo) {
        if (!window.confirm(`¿Eliminar la serie "${titulo}" y todos sus capítulos?`)) return
        try {
            await adminApi.eliminarAnime(id)
            mostrarAviso(`Serie "${titulo}" eliminada.`)
            await cargarTodo()
        } catch (err) {
            setError(err.message)
        }
    }

    // =========================================================================
    // Acciones de Capítulos y Subida S3
    // =========================================================================
    async function manejarSubidaS3(e) {
        e.preventDefault()
        if (!archivoVideo) {
            setError('Selecciona un archivo .mp4 primero')
            return
        }
        setSubiendoVideo(true)
        setError(null)
        try {
            const res = await adminApi.subirVideoS3(archivoVideo, 'demo')
            setFormEpisodio((prev) => ({ ...prev, videoKey: res.key }))
            mostrarAviso(`Video subido a AWS S3: ${res.key}`)
            const listaS3 = await adminApi.listarVideosS3().catch(() => [])
            setVideosS3(listaS3)
            setArchivoVideo(null)
        } catch (err) {
            setError(err.message || 'Error al subir archivo a S3')
        } finally {
            setSubiendoVideo(false)
        }
    }

    async function guardarEpisodio(e) {
        e.preventDefault()
        if (!animeSeleccionadoId) {
            setError('Selecciona una serie primero')
            return
        }
        if (!formEpisodio.videoKey) {
            setError('Debes indicar o subir un archivo de video en S3 (videoKey)')
            return
        }
        try {
            await adminApi.guardarEpisodio({
                animeId: Number(animeSeleccionadoId),
                numero: Number(formEpisodio.numero),
                titulo: formEpisodio.titulo,
                duracionSeg: Number(formEpisodio.duracionSeg),
                videoKey: formEpisodio.videoKey,
                licencia: formEpisodio.licencia,
                fuenteUrl: formEpisodio.fuenteUrl,
            })
            mostrarAviso(`Capítulo ${formEpisodio.numero} guardado exitosamente.`)
            const eps = await catalogoApi.episodios(animeSeleccionadoId)
            setEpisodios(eps || [])
            cambiarNumeroCapitulo(Number(formEpisodio.numero) + 1, animeSeleccionadoId)
        } catch (err) {
            setError(err.message || 'Error al guardar el capítulo')
        }
    }

    async function borrarEpisodio(id, numero) {
        if (!window.confirm(`¿Eliminar el capítulo ${numero}?`)) return
        try {
            await adminApi.eliminarEpisodio(id)
            mostrarAviso(`Capítulo ${numero} eliminado.`)
            const eps = await catalogoApi.episodios(animeSeleccionadoId)
            setEpisodios(eps || [])
        } catch (err) {
            setError(err.message)
        }
    }

    // =========================================================================
    // Acciones de Cuentas de Usuario
    // =========================================================================
    async function alternarActivoUsuario(u) {
        try {
            const actualizado = await adminApi.actualizarUsuario(u.id, { activo: !u.activo })
            setUsuarios((prev) => prev.map((item) => (item.id === u.id ? actualizado : item)))
            mostrarAviso(
                `Cuenta ${actualizado.email} ${actualizado.activo ? 'activada' : 'bloqueada'}.`
            )
        } catch (err) {
            setError(err.message)
        }
    }

    async function alternarRolAdmin(u) {
        const tieneAdmin =
            u.roles?.includes('ADMIN') || u.roles?.includes('ROLE_ADMIN')
        const nuevosRoles = tieneAdmin ? ['USUARIO'] : ['USUARIO', 'ADMIN']
        try {
            const actualizado = await adminApi.actualizarUsuario(u.id, { roles: nuevosRoles })
            setUsuarios((prev) => prev.map((item) => (item.id === u.id ? actualizado : item)))
            mostrarAviso(`Permisos actualizados para ${actualizado.email}.`)
        } catch (err) {
            setError(err.message)
        }
    }

    async function alternarSuscripcion(u) {
        const nuevoTipo = u.tipoSuscripcion === 'PREMIUM' ? 'GRATUITA' : 'PREMIUM'
        try {
            const actualizado = await adminApi.actualizarUsuario(u.id, {
                tipoSuscripcion: nuevoTipo,
            })
            setUsuarios((prev) => prev.map((item) => (item.id === u.id ? actualizado : item)))
            mostrarAviso(`Suscripción de ${actualizado.email} cambiada a ${nuevoTipo}.`)
        } catch (err) {
            setError(err.message)
        }
    }

    async function borrarUsuario(u) {
        if (!window.confirm(`¿Eliminar permanentemente la cuenta de ${u.email}?`)) return
        try {
            await adminApi.eliminarUsuario(u.id)
            setUsuarios((prev) => prev.filter((item) => item.id !== u.id))
            mostrarAviso(`Cuenta ${u.email} eliminada.`)
        } catch (err) {
            setError(err.message)
        }
    }

    // Filtros derivados
    const animesFiltrados = useMemo(() => {
        const q = filtroSeries.trim().toLowerCase()
        const filtrados = animes.filter((a) => !q || (a.titulo || '').toLowerCase().includes(q))
        if (ordenSeriesScore) {
            return [...filtrados].sort(
                (a, b) => Number(b.puntuacion || 0) - Number(a.puntuacion || 0)
            )
        }
        return filtrados
    }, [animes, filtroSeries, ordenSeriesScore])

    const animeSeleccionadoObj = useMemo(
        () => animes.find((a) => String(a.id) === String(animeSeleccionadoId)) || null,
        [animes, animeSeleccionadoId]
    )

    const usuariosFiltrados = useMemo(() => {
        const q = busquedaUsuario.trim().toLowerCase()
        return usuarios.filter((u) => {
            if (
                q &&
                !(u.nombre || '').toLowerCase().includes(q) &&
                !(u.email || '').toLowerCase().includes(q)
            ) {
                return false
            }
            if (filtroRol) {
                const limpio = filtroRol.replace('ROLE_', '')
                const tiene =
                    (u.roles || []).includes(limpio) ||
                    (u.roles || []).includes(`ROLE_${limpio}`)
                if (!tiene) return false
            }
            if (filtroPlan && u.tipoSuscripcion !== filtroPlan) {
                return false
            }
            return true
        })
    }, [usuarios, busquedaUsuario, filtroRol, filtroPlan])

    const kpiUsuarios = useMemo(() => {
        const total = usuarios.length
        const premium = usuarios.filter((u) => u.tipoSuscripcion === 'PREMIUM').length
        const admins = usuarios.filter(
            (u) => u.roles?.includes('ADMIN') || u.roles?.includes('ROLE_ADMIN')
        ).length
        const perfiles = usuarios.reduce(
            (acc, u) => acc + Number(u.cantidadPerfiles || 0),
            0
        )
        return { total, premium, admins, perfiles }
    }, [usuarios])

    if (cargando) return <Cargando texto="Cargando panel de administración..." />

    return (
        <main className="relative pt-24 pb-20 max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop w-full">
            {/* Ambient Stage Lighting Orbs */}
            <div className="fixed top-[-10%] left-[20%] w-[520px] h-[520px] rounded-full bg-primary-container/10 blur-[140px] pointer-events-none -z-10" />
            <div className="fixed bottom-[-10%] right-[10%] w-[440px] h-[440px] rounded-full bg-secondary-container/10 blur-[140px] pointer-events-none -z-10" />

            {/* PAGE HEADER & TELEMETRY */}
            <section className="mb-8 flex flex-col lg:flex-row lg:items-end justify-between gap-6">
                <div>
                    <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-low border border-outline-variant/30 mb-3">
                        <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                        <span className="text-label-sm uppercase tracking-wider text-primary">
                            Control Maestro
                        </span>
                        <span className="text-outline-variant">•</span>
                        <span className="text-label-sm text-secondary flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px]">cloud_done</span>
                            AWS S3 / CloudFront Activo
                        </span>
                    </div>
                    <h1 className="font-headline-xl text-3xl sm:text-4xl font-extrabold text-on-surface tracking-tight">
                        Panel de Administración
                    </h1>
                    <p className="mt-1.5 text-body-md text-on-surface-variant max-w-2xl">
                        Gestiona las series del catálogo, sube capítulos a AWS S3 y administra las cuentas de usuarios.
                    </p>
                </div>

                {/* Quick Telemetry Cards */}
                <div className="grid grid-cols-3 gap-3 sm:gap-4 shrink-0">
                    <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 py-3 flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
                            <span className="material-symbols-outlined">movie</span>
                        </div>
                        <div>
                            <p className="text-caption text-on-surface-variant">Series</p>
                            <p className="font-headline-xl text-xl font-bold text-on-surface">{animes.length}</p>
                        </div>
                    </div>
                    <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 py-3 flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-secondary/10 border border-secondary/20 flex items-center justify-center text-secondary">
                            <span className="material-symbols-outlined">cloud_done</span>
                        </div>
                        <div>
                            <p className="text-caption text-on-surface-variant">Videos S3</p>
                            <p className="font-headline-xl text-xl font-bold text-on-surface">{videosS3.length}</p>
                        </div>
                    </div>
                    <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 py-3 flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-tertiary/10 border border-tertiary/20 flex items-center justify-center text-tertiary">
                            <span className="material-symbols-outlined">group</span>
                        </div>
                        <div>
                            <p className="text-caption text-on-surface-variant">Cuentas</p>
                            <p className="font-headline-xl text-xl font-bold text-on-surface">{usuarios.length}</p>
                        </div>
                    </div>
                </div>
            </section>

            {/* SUB-NAVIGATION TABS */}
            <div className="mb-8 border-b border-outline-variant/25 flex items-center justify-between flex-wrap gap-4">
                <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-3">
                    <button
                        type="button"
                        onClick={() => setTab('series')}
                        className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-label-md transition-all ${
                            tab === 'series'
                                ? 'bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_0_24px_rgba(160,120,255,0.35)]'
                                : 'bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface border border-outline-variant/20'
                        }`}
                    >
                        <span className="material-symbols-outlined text-[18px]">video_library</span>
                        <span>Series ({animes.length})</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => setTab('capitulos')}
                        className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-label-md transition-all ${
                            tab === 'capitulos'
                                ? 'bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_0_24px_rgba(160,120,255,0.35)]'
                                : 'bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface border border-outline-variant/20'
                        }`}
                    >
                        <span className="material-symbols-outlined text-[18px]">cloud_upload</span>
                        <span>Capítulos y AWS S3</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => setTab('cuentas')}
                        className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-label-md transition-all ${
                            tab === 'cuentas'
                                ? 'bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_0_24px_rgba(160,120,255,0.35)]'
                                : 'bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface border border-outline-variant/20'
                        }`}
                    >
                        <span className="material-symbols-outlined text-[18px]">manage_accounts</span>
                        <span>Cuentas de Usuario ({usuarios.length})</span>
                    </button>
                </div>
            </div>

            {/* NOTIFICATIONS */}
            {mensaje && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-300 flex items-center gap-3 shadow-lg">
                    <span className="material-symbols-outlined">check_circle</span>
                    <span className="text-label-md font-medium">{mensaje}</span>
                </div>
            )}
            {error && (
                <div className="mb-6 p-4 rounded-xl bg-error-container/30 border border-error/40 text-error flex items-center gap-3 shadow-lg">
                    <span className="material-symbols-outlined">error</span>
                    <span className="text-label-md font-medium">{error}</span>
                </div>
            )}

            {/* =====================================================================
                PESTAÑA 1: SERIES
               ===================================================================== */}
            {tab === 'series' && (
                <div>
                    {/* SMART INGESTION BOX: AGREGAR NUEVA SERIE POR NOMBRE */}
                    <section className="mb-10 relative rounded-2xl bg-surface-container-low border border-outline-variant/30 p-6 sm:p-8 shadow-2xl overflow-hidden">
                        <div className="absolute top-0 inset-x-0 h-[1px] bg-gradient-to-r from-transparent via-primary/50 to-transparent" />
                        <div className="relative z-10">
                            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
                                <div className="flex items-start gap-3.5">
                                    <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary/20 to-primary-container/10 border border-primary/30 flex items-center justify-center text-primary shrink-0 mt-0.5">
                                        <span className="material-symbols-outlined text-[24px]">auto_awesome</span>
                                    </div>
                                    <div>
                                        <div className="flex items-center gap-2.5 flex-wrap">
                                            <h2 className="font-headline-xl text-xl font-bold text-on-surface">
                                                Agregar nueva serie por nombre
                                            </h2>
                                            <span className="px-2.5 py-0.5 rounded-full text-label-sm bg-primary/15 text-primary border border-primary/30">
                                                Auto-Sync API + LibreTranslate
                                            </span>
                                        </div>
                                        <p className="text-caption text-on-surface-variant mt-1">
                                            Escribe únicamente el nombre del anime y el sistema traerá automáticamente su portada, sinopsis traducida al español, año, estado, episodios, puntaje y géneros desde la API.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Search & Action Row */}
                            <form
                                onSubmit={importarDirectoPorNombre}
                                className="flex flex-col lg:flex-row items-stretch lg:items-center gap-3"
                            >
                                <div className="relative flex-1">
                                    <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline text-[20px]">
                                        search
                                    </span>
                                    <input
                                        type="text"
                                        value={nombreBusquedaJikan}
                                        onChange={(e) => setNombreBusquedaJikan(e.target.value)}
                                        placeholder="Ej: Solo Leveling, Dandadan, One Piece, Bleach..."
                                        className="w-full pl-11 pr-4 py-3.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-body-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                                    />
                                </div>

                                <div className="flex flex-wrap sm:flex-nowrap items-center gap-2.5">
                                    <button
                                        type="submit"
                                        disabled={importandoJikan || !nombreBusquedaJikan.trim()}
                                        className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_0_20px_rgba(160,120,255,0.3)] hover:shadow-[0_0_28px_rgba(160,120,255,0.5)] disabled:opacity-40 transition-all whitespace-nowrap"
                                    >
                                        <span className="material-symbols-outlined text-[20px]">add_circle</span>
                                        <span>{importandoJikan ? 'Importando...' : 'Agregar serie'}</span>
                                    </button>

                                    <button
                                        type="button"
                                        disabled={buscandoJikan || !nombreBusquedaJikan.trim()}
                                        onClick={buscarOpcionesEnJikan}
                                        className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-4 py-3.5 rounded-xl bg-surface-container-high hover:bg-surface-bright text-on-surface font-medium text-label-md border border-outline-variant/30 disabled:opacity-40 transition-all whitespace-nowrap"
                                    >
                                        <span className="material-symbols-outlined text-[18px] text-secondary">
                                            manage_search
                                        </span>
                                        <span>{buscandoJikan ? 'Buscando...' : 'Elegir entre coincidencias'}</span>
                                    </button>
                                </div>
                            </form>

                            {/* Resultados de coincidencias */}
                            {resultadosJikan.length > 0 && (
                                <div className="mt-5 p-4 rounded-xl bg-surface-container border border-outline-variant/30">
                                    <div className="flex items-center justify-between mb-3">
                                        <span className="text-label-sm uppercase tracking-wider text-primary">
                                            Selecciona la serie que deseas importar:
                                        </span>
                                        <button
                                            type="button"
                                            onClick={() => setResultadosJikan([])}
                                            className="text-xs text-outline hover:text-on-surface"
                                        >
                                            Cerrar lista
                                        </button>
                                    </div>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                        {resultadosJikan.map((cand) => {
                                            const malId = cand.mal_id ?? cand.malId
                                            const tituloCand = cand.title || cand.titulo
                                            const imgCand =
                                                cand.images?.jpg?.large_image_url ||
                                                cand.images?.jpg?.image_url ||
                                                cand.imagenUrl
                                            const anioCand =
                                                cand.aired?.prop?.from?.year || cand.anio || '—'
                                            const scoreCand = cand.score ?? cand.puntuacion ?? '—'
                                            const epsCand = cand.episodes ?? cand.episodios ?? '?'

                                            return (
                                                <div
                                                    key={malId || tituloCand}
                                                    className="flex items-center justify-between gap-3 p-3 rounded-xl bg-surface-container-low border border-outline-variant/20"
                                                >
                                                    <div className="flex items-center gap-3 min-w-0">
                                                        {imgCand && (
                                                            <img
                                                                src={imgCand}
                                                                alt={tituloCand}
                                                                className="w-12 h-16 object-cover rounded-lg shrink-0"
                                                            />
                                                        )}
                                                        <div className="min-w-0">
                                                            <p className="font-title-card text-sm font-bold text-on-surface truncate">
                                                                {tituloCand}
                                                            </p>
                                                            <p className="text-caption text-on-surface-variant">
                                                                {anioCand} · {epsCand} eps · ★ {scoreCand}
                                                            </p>
                                                        </div>
                                                    </div>
                                                    <button
                                                        type="button"
                                                        disabled={importandoJikan}
                                                        onClick={() => importarCandidatoJikan(cand)}
                                                        className="px-3.5 py-2 rounded-lg bg-primary text-on-primary font-bold text-xs shrink-0 hover:brightness-110 disabled:opacity-40 transition-all"
                                                    >
                                                        Importar
                                                    </button>
                                                </div>
                                            )
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Utility Actions Footer Bar */}
                            <div className="mt-5 pt-4 border-t border-outline-variant/20 flex flex-wrap items-center justify-between gap-3">
                                <div className="flex flex-wrap items-center gap-2">
                                    <button
                                        type="button"
                                        onClick={() => setMostrarFormManual(!mostrarFormManual)}
                                        className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface text-caption font-medium border border-outline-variant/20 transition-colors"
                                    >
                                        <span className="material-symbols-outlined text-[16px] text-primary">
                                            edit_note
                                        </span>
                                        <span>
                                            {mostrarFormManual ? 'Ocultar formulario manual' : 'Formulario manual'}
                                        </span>
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() =>
                                            adminApi
                                                .traducirPendientes()
                                                .then(() => {
                                                    mostrarAviso(
                                                        'Proceso de traducción de sinopsis ejecutado correctamente.'
                                                    )
                                                    cargarTodo()
                                                })
                                                .catch((e) => setError(e.message))
                                        }
                                        className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface text-caption font-medium border border-outline-variant/20 transition-colors"
                                    >
                                        <span className="material-symbols-outlined text-[16px] text-tertiary">
                                            translate
                                        </span>
                                        <span>Traducir sinopsis pendientes</span>
                                    </button>
                                </div>
                            </div>

                            {/* Formulario manual o de edición desplegable */}
                            {mostrarFormManual && (
                                <form
                                    onSubmit={guardarAnime}
                                    className="mt-6 pt-6 border-t border-outline-variant/30 space-y-4"
                                >
                                    <h3 className="font-title-card text-lg font-bold text-on-surface">
                                        {editandoAnimeId
                                            ? `Editando serie #${editandoAnimeId}`
                                            : 'Crear serie manualmente'}
                                    </h3>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                Título
                                            </label>
                                            <input
                                                type="text"
                                                required
                                                value={formAnime.titulo}
                                                onChange={(e) =>
                                                    setFormAnime({ ...formAnime, titulo: e.target.value })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                Título Japonés (opcional)
                                            </label>
                                            <input
                                                type="text"
                                                value={formAnime.tituloJapones}
                                                onChange={(e) =>
                                                    setFormAnime({
                                                        ...formAnime,
                                                        tituloJapones: e.target.value,
                                                    })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                URL de Portada (imagenKey)
                                            </label>
                                            <input
                                                type="text"
                                                required
                                                value={formAnime.imagenKey}
                                                onChange={(e) =>
                                                    setFormAnime({ ...formAnime, imagenKey: e.target.value })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                Año
                                            </label>
                                            <input
                                                type="number"
                                                value={formAnime.anio}
                                                onChange={(e) =>
                                                    setFormAnime({ ...formAnime, anio: e.target.value })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                Estado
                                            </label>
                                            <select
                                                value={formAnime.estado}
                                                onChange={(e) =>
                                                    setFormAnime({ ...formAnime, estado: e.target.value })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            >
                                                <option value="EN_EMISION">EN_EMISION</option>
                                                <option value="FINALIZADO">FINALIZADO</option>
                                                <option value="PROXIMAMENTE">PROXIMAMENTE</option>
                                            </select>
                                        </div>
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                                Puntuación (0 a 10)
                                            </label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                min="0"
                                                max="10"
                                                value={formAnime.puntuacion}
                                                onChange={(e) =>
                                                    setFormAnime({ ...formAnime, puntuacion: e.target.value })
                                                }
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                            />
                                        </div>
                                    </div>

                                    {generosCatalogo.length > 0 && (
                                        <div>
                                            <label className="text-label-sm text-on-surface-variant uppercase block mb-2">
                                                Géneros
                                            </label>
                                            <div className="flex flex-wrap gap-2">
                                                {generosCatalogo.map((g) => {
                                                    const activo = formAnime.generoIds.includes(g.id)
                                                    return (
                                                        <button
                                                            key={g.id}
                                                            type="button"
                                                            onClick={() => toggleGeneroId(g.id)}
                                                            className={`px-3 py-1 rounded-lg text-xs font-medium border transition-all ${
                                                                activo
                                                                    ? 'bg-primary text-on-primary border-primary font-bold'
                                                                    : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/40'
                                                            }`}
                                                        >
                                                            {g.nombre}
                                                        </button>
                                                    )
                                                })}
                                            </div>
                                        </div>
                                    )}

                                    <div>
                                        <label className="text-label-sm text-on-surface-variant uppercase block mb-1">
                                            Sinopsis en español
                                        </label>
                                        <textarea
                                            rows={3}
                                            required
                                            value={formAnime.sinopsis}
                                            onChange={(e) =>
                                                setFormAnime({ ...formAnime, sinopsis: e.target.value })
                                            }
                                            className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md"
                                        />
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <button
                                            type="submit"
                                            className="px-5 py-2.5 rounded-xl bg-primary text-on-primary font-bold text-label-md"
                                        >
                                            {editandoAnimeId ? 'Guardar cambios' : 'Crear serie'}
                                        </button>
                                        <button
                                            type="button"
                                            onClick={limpiarFormAnime}
                                            className="px-4 py-2.5 rounded-xl bg-surface-container-high text-on-surface text-label-md"
                                        >
                                            Cancelar
                                        </button>
                                    </div>
                                </form>
                            )}
                        </div>
                    </section>

                    {/* CATALOG SERIES TABLE SECTION */}
                    <section className="rounded-2xl bg-surface-container-low border border-outline-variant/25 shadow-2xl overflow-hidden">
                        <div className="p-6 border-b border-outline-variant/20 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                            <div>
                                <h2 className="font-headline-xl text-xl font-bold text-on-surface">
                                    Series en el catálogo ({animesFiltrados.length})
                                </h2>
                                <p className="text-caption text-on-surface-variant mt-0.5">
                                    Listado maestro sincronizado con la base de datos y CDN de portadas.
                                </p>
                            </div>

                            <div className="flex items-center gap-3">
                                <div className="relative">
                                    <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">
                                        filter_list
                                    </span>
                                    <input
                                        type="text"
                                        value={filtroSeries}
                                        onChange={(e) => setFiltroSeries(e.target.value)}
                                        placeholder="Filtrar en tabla..."
                                        className="pl-9 pr-3.5 py-2 rounded-lg bg-surface-container-lowest border border-outline-variant/30 text-caption text-on-surface placeholder-outline focus:outline-none focus:border-primary w-48 sm:w-64 transition-all"
                                    />
                                </div>
                                <button
                                    type="button"
                                    onClick={() => setOrdenSeriesScore(!ordenSeriesScore)}
                                    className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface text-caption border border-outline-variant/20 transition-colors"
                                >
                                    <span className="material-symbols-outlined text-[16px]">sort</span>
                                    <span className="hidden sm:inline">
                                        {ordenSeriesScore ? 'Puntaje ↓' : 'Recientes'}
                                    </span>
                                </button>
                            </div>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-surface-container/60 border-b border-outline-variant/20 text-label-sm uppercase tracking-wider text-outline">
                                        <th className="py-3.5 pl-6 pr-3 font-semibold">Portada</th>
                                        <th className="py-3.5 px-3 font-semibold">Título</th>
                                        <th className="py-3.5 px-3 font-semibold">Año</th>
                                        <th className="py-3.5 px-3 font-semibold">Puntaje</th>
                                        <th className="py-3.5 px-3 font-semibold">Géneros</th>
                                        <th className="py-3.5 pl-3 pr-6 text-right font-semibold">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/15 text-label-md">
                                    {animesFiltrados.map((a) => (
                                        <tr
                                            key={a.id}
                                            className="group hover:bg-surface-container-high/50 transition-colors"
                                        >
                                            <td className="py-3.5 pl-6 pr-3 w-20">
                                                <div className="w-12 h-16 rounded-lg overflow-hidden bg-surface-container-highest border border-outline-variant/30 shadow-md">
                                                    {a.imagenUrl && (
                                                        <img
                                                            src={a.imagenUrl}
                                                            alt={a.titulo}
                                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                                                        />
                                                    )}
                                                </div>
                                            </td>
                                            <td className="py-3.5 px-3">
                                                <div className="font-title-card text-on-surface font-semibold text-base group-hover:text-primary transition-colors">
                                                    {a.titulo}
                                                </div>
                                                <div className="text-caption text-outline mt-0.5 flex items-center gap-2">
                                                    <span>ID #{a.id}</span>
                                                    <span>•</span>
                                                    <span className="text-emerald-400">
                                                        {a.estado || 'Publicado'}
                                                    </span>
                                                </div>
                                            </td>
                                            <td className="py-3.5 px-3 text-on-surface-variant font-medium">
                                                {a.anio || '—'}
                                            </td>
                                            <td className="py-3.5 px-3">
                                                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-tertiary/15 text-tertiary border border-tertiary/30 text-label-sm font-bold">
                                                    ★ {a.puntuacion ?? '—'}
                                                </span>
                                            </td>
                                            <td className="py-3.5 px-3">
                                                <div className="flex flex-wrap gap-1.5">
                                                    {(a.generos || []).map((g, idx) => (
                                                        <span
                                                            key={idx}
                                                            className="px-2.5 py-0.5 rounded-md bg-surface-container-highest text-on-surface-variant text-caption"
                                                        >
                                                            {typeof g === 'string' ? g : g.nombre}
                                                        </span>
                                                    ))}
                                                </div>
                                            </td>
                                            <td className="py-3.5 pl-3 pr-6 text-right">
                                                <div className="inline-flex items-center justify-end gap-2">
                                                    <button
                                                        type="button"
                                                        onClick={() => seleccionarParaEditar(a)}
                                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-highest hover:bg-surface-bright text-on-surface text-caption font-medium transition-colors"
                                                    >
                                                        <span className="material-symbols-outlined text-[15px]">
                                                            edit
                                                        </span>
                                                        <span>Editar</span>
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            setAnimeSeleccionadoId(String(a.id))
                                                            setTab('capitulos')
                                                        }}
                                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary/15 hover:bg-primary/25 text-primary border border-primary/30 text-caption font-semibold transition-colors"
                                                    >
                                                        <span className="material-symbols-outlined text-[15px]">
                                                            playlist_play
                                                        </span>
                                                        <span>Capítulos</span>
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => borrarAnime(a.id, a.titulo)}
                                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-error-container/25 hover:bg-error-container/50 text-error border border-error/25 text-caption font-medium transition-colors"
                                                    >
                                                        <span className="material-symbols-outlined text-[15px]">
                                                            delete
                                                        </span>
                                                        <span>Eliminar</span>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                </div>
            )}

            {/* =====================================================================
                PESTAÑA 2: CAPÍTULOS Y AWS S3
               ===================================================================== */}
            {tab === 'capitulos' && (
                <div className="space-y-8">
                    {/* STEP 1: SELECT SERIES CARD */}
                    <section className="bg-surface-container-low rounded-2xl p-6 border border-outline-variant/25 shadow-xl relative overflow-hidden">
                        <div className="absolute top-0 left-0 w-1 h-full bg-gradient-to-b from-primary to-primary-container" />
                        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                            <div className="space-y-1">
                                <div className="flex items-center gap-2.5">
                                    <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-label-sm font-bold">
                                        1
                                    </span>
                                    <h2 className="font-title-card text-lg font-bold text-on-surface">
                                        1. Selecciona la serie
                                    </h2>
                                </div>
                                <p className="text-caption text-on-surface-variant pl-8">
                                    Elige el anime al que deseas vincular o subir nuevos episodios en el bucket S3.
                                </p>
                            </div>

                            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 flex-1 max-w-2xl">
                                <div className="relative flex-1">
                                    <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-primary text-[20px] pointer-events-none">
                                        theaters
                                    </span>
                                    <select
                                        aria-label="Seleccionar serie"
                                        value={animeSeleccionadoId}
                                        onChange={(e) => setAnimeSeleccionadoId(e.target.value)}
                                        className="w-full pl-11 pr-10 py-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md font-semibold focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all cursor-pointer"
                                    >
                                        {animes.map((a) => (
                                            <option key={a.id} value={a.id}>
                                                {a.titulo} (ID #{a.id})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {animeSeleccionadoObj && (
                                    <div className="flex items-center gap-3 px-3.5 py-2 rounded-xl bg-surface-container border border-outline-variant/20 shrink-0">
                                        {animeSeleccionadoObj.imagenUrl && (
                                            <img
                                                src={animeSeleccionadoObj.imagenUrl}
                                                alt={animeSeleccionadoObj.titulo}
                                                className="w-9 h-12 object-cover rounded-md border border-outline-variant/30"
                                            />
                                        )}
                                        <div className="text-left">
                                            <div className="text-label-sm text-primary uppercase tracking-wider">
                                                Serie Activa
                                            </div>
                                            <div className="text-caption text-on-surface font-semibold">
                                                {episodios.length} cap(s) registrados · {animeSeleccionadoObj.anio}
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </section>

                    {/* WORKSPACE GRID: STEP 2 (S3 VIDEO SOURCE) + STEP 3 (CHAPTER METADATA) */}
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                        {/* STEP 2: ORIGEN DEL VIDEO EN AWS S3 (5 cols) */}
                        <section className="lg:col-span-5 bg-surface-container-low rounded-2xl p-6 border border-outline-variant/25 shadow-xl flex flex-col gap-6 relative overflow-hidden">
                            <div className="absolute top-0 left-0 w-1 h-full bg-gradient-to-b from-secondary to-secondary-container" />
                            <div>
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-2.5">
                                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-secondary/20 text-secondary text-label-sm font-bold">
                                            2
                                        </span>
                                        <h2 className="font-title-card text-lg font-bold text-on-surface">
                                            2. Origen del video en AWS S3
                                        </h2>
                                    </div>
                                    <span className="px-2.5 py-0.5 rounded-md bg-secondary-container/20 border border-secondary/30 text-secondary text-label-sm">
                                        sugoianime-media
                                    </span>
                                </div>
                                <p className="text-caption text-on-surface-variant mt-1 pl-8">
                                    Sube un archivo nuevo o reutiliza un objeto existente en el bucket.
                                </p>
                            </div>

                            {/* Opción A: Subir archivo .mp4 directo */}
                            <form
                                onSubmit={manejarSubidaS3}
                                className="p-4 rounded-xl bg-surface-container border border-outline-variant/25 space-y-4"
                            >
                                <div className="flex items-center justify-between">
                                    <span className="text-label-md font-semibold text-on-surface flex items-center gap-2">
                                        <span className="material-symbols-outlined text-primary text-[18px]">
                                            upload_file
                                        </span>
                                        Opción A: Subir archivo .mp4 desde tu equipo
                                    </span>
                                    <span className="text-label-sm text-outline">Máx. 500 MB</span>
                                </div>

                                <label className="group flex flex-col items-center justify-center border-2 border-dashed border-outline-variant/50 hover:border-primary/70 rounded-xl p-5 bg-surface-container-lowest/60 hover:bg-surface-container-lowest transition-all cursor-pointer text-center">
                                    <div className="w-11 h-11 rounded-full bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center text-primary mb-2.5 transition-colors">
                                        <span className="material-symbols-outlined text-[24px]">cloud_upload</span>
                                    </div>
                                    <span className="text-label-md text-on-surface font-medium">
                                        {archivoVideo ? archivoVideo.name : 'Selecciona o arrastra un video .mp4'}
                                    </span>
                                    <span className="text-caption text-outline mt-0.5">
                                        Se almacenará en{' '}
                                        <code className="text-secondary font-mono">s3://sugoianime-media/demo/</code>
                                    </span>
                                    <input
                                        type="file"
                                        accept="video/mp4,video/webm"
                                        onChange={(e) => setArchivoVideo(e.target.files?.[0] || null)}
                                        className="hidden"
                                    />
                                </label>

                                <button
                                    type="submit"
                                    disabled={subiendoVideo || !archivoVideo}
                                    className="w-full py-2.5 px-4 rounded-xl bg-surface-container-high hover:bg-surface-bright disabled:opacity-40 text-on-surface hover:text-primary border border-outline-variant/40 text-label-md font-semibold flex items-center justify-center gap-2 transition-all"
                                >
                                    <span className="material-symbols-outlined text-[18px]">publish</span>
                                    <span>{subiendoVideo ? 'Subiendo a S3...' : 'Subir a S3'}</span>
                                </button>
                            </form>

                            {/* Opción B: Elegir archivo existente en S3 */}
                            <div className="p-4 rounded-xl bg-surface-container border border-outline-variant/25 space-y-3">
                                <div className="flex items-center justify-between">
                                    <span className="text-label-md font-semibold text-on-surface flex items-center gap-2">
                                        <span className="material-symbols-outlined text-secondary text-[18px]">
                                            folder_open
                                        </span>
                                        Opción B: Elegir archivo ya existente en el bucket S3 ({videosS3.length})
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => adminApi.listarVideosS3().then(setVideosS3)}
                                        className="inline-flex items-center gap-1 text-caption text-secondary hover:text-primary transition-colors"
                                    >
                                        <span className="material-symbols-outlined text-[15px]">refresh</span>
                                        <span>Actualizar lista</span>
                                    </button>
                                </div>

                                <div className="space-y-2 max-h-[260px] overflow-y-auto pr-1 custom-scrollbar">
                                    {videosS3.map((v) => {
                                        const seleccionado = formEpisodio.videoKey === v.key
                                        const bytes = v.tamanoBytes ?? v.size ?? 0
                                        return (
                                            <div
                                                key={v.key}
                                                onClick={() =>
                                                    setFormEpisodio((prev) => ({ ...prev, videoKey: v.key }))
                                                }
                                                className={`flex items-center justify-between gap-2 p-2.5 rounded-lg cursor-pointer transition-all ${
                                                    seleccionado
                                                        ? 'bg-primary/10 border border-primary/40'
                                                        : 'bg-surface-container-lowest hover:bg-surface-container-high/60 border border-outline-variant/25'
                                                }`}
                                            >
                                                <div className="flex items-center gap-2.5 min-w-0">
                                                    <span
                                                        className={`material-symbols-outlined text-[18px] shrink-0 ${
                                                            seleccionado ? 'text-primary' : 'text-outline'
                                                        }`}
                                                    >
                                                        {seleccionado ? 'check_circle' : 'movie'}
                                                    </span>
                                                    <div className="min-w-0">
                                                        <div className="text-caption font-mono text-on-surface truncate">
                                                            {v.key}
                                                        </div>
                                                        <div className="text-[11px] text-outline">
                                                            {(bytes / (1024 * 1024)).toFixed(2)} MB
                                                        </div>
                                                    </div>
                                                </div>
                                                <span
                                                    className={`px-2.5 py-1 rounded-md text-label-sm shrink-0 ${
                                                        seleccionado
                                                            ? 'bg-primary text-on-primary font-bold'
                                                            : 'bg-surface-container-high text-on-surface'
                                                    }`}
                                                >
                                                    {seleccionado ? 'Seleccionado' : 'Usar'}
                                                </span>
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                        </section>

                        {/* STEP 3: DATOS DEL CAPÍTULO (AUTO-FILLED FORM) (7 cols) */}
                        <section className="lg:col-span-7 bg-surface-container-low rounded-2xl p-6 border border-outline-variant/25 shadow-xl relative overflow-hidden">
                            <div className="absolute top-0 left-0 w-1 h-full bg-gradient-to-b from-primary via-primary-container to-tertiary" />

                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
                                <div>
                                    <div className="flex items-center gap-2.5">
                                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-label-sm font-bold">
                                            3
                                        </span>
                                        <h2 className="font-title-card text-lg font-bold text-on-surface">
                                            3. Datos del capítulo
                                        </h2>
                                    </div>
                                    <p className="text-caption text-on-surface-variant mt-1 pl-8">
                                        Al elegir el número del capítulo, el título, duración, licencia y fuente se llenan automáticamente.
                                    </p>
                                </div>

                                <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-primary/15 border border-primary/30 text-primary text-label-sm shrink-0 self-start sm:self-auto">
                                    <span className="material-symbols-outlined text-[16px]">auto_awesome</span>
                                    <span>
                                        {autocompletandoCapitulo
                                            ? 'Autocompletando desde la API...'
                                            : capituloAutocompletado
                                            ? 'Datos autocompletados'
                                            : 'Auto-llenado inteligente'}
                                    </span>
                                </div>
                            </div>

                            <form onSubmit={guardarEpisodio} className="space-y-5">
                                <div className="grid grid-cols-1 sm:grid-cols-12 gap-4">
                                    {/* Número de capítulo */}
                                    <div className="sm:col-span-5 space-y-1.5">
                                        <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                            Número de capítulo
                                        </label>
                                        <div className="flex gap-2">
                                            <select
                                                value={formEpisodio.numero}
                                                onChange={(e) => cambiarNumeroCapitulo(e.target.value)}
                                                className="flex-1 px-3 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md font-semibold focus:outline-none focus:border-primary"
                                            >
                                                {Array.from(
                                                    {
                                                        length: Math.max(
                                                            totalEpisodiosSugeridos || 24,
                                                            24
                                                        ),
                                                    },
                                                    (_, idx) => idx + 1
                                                ).map((num) => (
                                                    <option key={num} value={num}>
                                                        Capítulo {num}
                                                    </option>
                                                ))}
                                            </select>
                                            <input
                                                type="number"
                                                min="1"
                                                value={formEpisodio.numero}
                                                onChange={(e) => cambiarNumeroCapitulo(e.target.value)}
                                                title="Escribir otro número de capítulo"
                                                className="w-20 px-3 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md font-bold text-center"
                                            />
                                        </div>
                                    </div>

                                    {/* Título del capítulo */}
                                    <div className="sm:col-span-7 space-y-1.5">
                                        <div className="flex items-center justify-between">
                                            <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                                Título del capítulo
                                            </label>
                                            {capituloAutocompletado && (
                                                <span className="text-[11px] text-primary flex items-center gap-1">
                                                    <span className="material-symbols-outlined text-[13px]">
                                                        verified
                                                    </span>
                                                    Sincronizado
                                                </span>
                                            )}
                                        </div>
                                        <input
                                            type="text"
                                            required
                                            value={formEpisodio.titulo}
                                            onChange={(e) =>
                                                setFormEpisodio({ ...formEpisodio, titulo: e.target.value })
                                            }
                                            className="w-full px-3.5 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md focus:outline-none focus:border-primary transition-all"
                                        />
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-12 gap-4">
                                    {/* Duración (segundos) */}
                                    <div className="sm:col-span-4 space-y-1.5">
                                        <div className="flex items-center justify-between">
                                            <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                                Duración (seg)
                                            </label>
                                            <span className="text-[11px] text-outline">
                                                ≈ {Math.round((formEpisodio.duracionSeg || 1440) / 60)} min
                                            </span>
                                        </div>
                                        <input
                                            type="number"
                                            min="1"
                                            value={formEpisodio.duracionSeg}
                                            onChange={(e) =>
                                                setFormEpisodio({
                                                    ...formEpisodio,
                                                    duracionSeg: e.target.value,
                                                })
                                            }
                                            className="w-full px-3.5 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md font-mono"
                                        />
                                    </div>

                                    {/* Licencia */}
                                    <div className="sm:col-span-8 space-y-1.5">
                                        <div className="flex items-center justify-between">
                                            <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                                Licencia (Ley 17.336)
                                            </label>
                                            <span className="text-[11px] text-tertiary">Atribución requerida</span>
                                        </div>
                                        <input
                                            type="text"
                                            required
                                            value={formEpisodio.licencia}
                                            onChange={(e) =>
                                                setFormEpisodio({ ...formEpisodio, licencia: e.target.value })
                                            }
                                            className="w-full px-3.5 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface text-label-md"
                                        />
                                    </div>
                                </div>

                                {/* Clave S3 (videoKey) */}
                                <div className="space-y-1.5">
                                    <div className="flex items-center justify-between">
                                        <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                            Clave S3 (videoKey)
                                        </label>
                                        <span className="text-[11px] text-secondary font-mono">
                                            CloudFront CDN Ready
                                        </span>
                                    </div>
                                    <div className="relative">
                                        <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-primary text-[18px]">
                                            link
                                        </span>
                                        <input
                                            type="text"
                                            required
                                            placeholder="demo/Cap_01_Shingeki_no_Kyojin.mp4"
                                            value={formEpisodio.videoKey}
                                            onChange={(e) =>
                                                setFormEpisodio({ ...formEpisodio, videoKey: e.target.value })
                                            }
                                            className="w-full pl-10 pr-4 py-2.5 bg-surface-container-lowest border border-primary/40 rounded-xl text-primary font-mono text-label-md focus:outline-none focus:border-primary"
                                        />
                                    </div>
                                </div>

                                {/* URL fuente */}
                                <div className="space-y-1.5">
                                    <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                                        URL fuente / atribución (opcional)
                                    </label>
                                    <input
                                        type="url"
                                        value={formEpisodio.fuenteUrl}
                                        onChange={(e) =>
                                            setFormEpisodio({ ...formEpisodio, fuenteUrl: e.target.value })
                                        }
                                        placeholder="https://anilist.co/anime/..."
                                        className="w-full px-3.5 py-2.5 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface-variant text-label-md"
                                    />
                                </div>

                                <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 border-t border-outline-variant/20">
                                    <div className="text-caption text-outline flex items-center gap-1.5">
                                        <span className="material-symbols-outlined text-[16px] text-emerald-400">
                                            verified_user
                                        </span>
                                        <span>Listo para publicar en el reproductor de la serie</span>
                                    </div>
                                    <button
                                        type="submit"
                                        className="px-6 py-3 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_8px_24px_rgba(160,120,255,0.3)] hover:shadow-[0_12px_32px_rgba(160,120,255,0.45)] flex items-center justify-center gap-2 transition-all"
                                    >
                                        <span className="material-symbols-outlined text-[20px]">save</span>
                                        <span>Guardar capítulo</span>
                                    </button>
                                </div>
                            </form>
                        </section>
                    </div>

                    {/* SECTION 4: CAPÍTULOS ACTUALES TABLE */}
                    <section className="bg-surface-container-low rounded-2xl border border-outline-variant/25 shadow-xl overflow-hidden">
                        <div className="p-6 border-b border-outline-variant/20 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 rounded-xl bg-primary/15 border border-primary/30 flex items-center justify-center text-primary">
                                    <span className="material-symbols-outlined">playlist_play</span>
                                </div>
                                <div>
                                    <h2 className="font-title-card text-lg font-bold text-on-surface">
                                        Capítulos actuales de la serie seleccionada ({episodios.length})
                                    </h2>
                                    <p className="text-caption text-on-surface-variant">
                                        Episodios vinculados a{' '}
                                        <strong className="text-on-surface">
                                            {animeSeleccionadoObj?.titulo || `#${animeSeleccionadoId}`}
                                        </strong>
                                    </p>
                                </div>
                            </div>
                        </div>

                        {episodios.length === 0 ? (
                            <div className="p-8 text-center text-on-surface-variant text-sm">
                                Esta serie aún no tiene capítulos cargados.
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-surface-container/70 border-b border-outline-variant/25 text-label-sm text-outline uppercase tracking-wider">
                                            <th className="py-3.5 px-6">#</th>
                                            <th className="py-3.5 px-4">Título</th>
                                            <th className="py-3.5 px-4">Duración</th>
                                            <th className="py-3.5 px-4">Licencia</th>
                                            <th className="py-3.5 px-6 text-right">Acciones</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/15 text-label-md">
                                        {episodios.map((ep) => (
                                            <tr
                                                key={ep.id}
                                                className="hover:bg-surface-container-high/40 transition-colors"
                                            >
                                                <td className="py-4 px-6">
                                                    <span className="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-primary/15 text-primary font-bold text-label-md">
                                                        {ep.numero}
                                                    </span>
                                                </td>
                                                <td className="py-4 px-4">
                                                    <div className="font-semibold text-on-surface">{ep.titulo}</div>
                                                </td>
                                                <td className="py-4 px-4 text-on-surface-variant">
                                                    {Math.round(
                                                        (ep.duracionSeg ?? ep.duracionSegundos ?? 1440) / 60
                                                    )}{' '}
                                                    min
                                                </td>
                                                <td className="py-4 px-4">
                                                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-tertiary/15 text-tertiary text-label-sm">
                                                        {ep.licencia || 'CC-BY-3.0'}
                                                    </span>
                                                </td>
                                                <td className="py-4 px-6 text-right">
                                                    <div className="inline-flex items-center justify-end gap-2">
                                                        <button
                                                            type="button"
                                                            onClick={() => navegar(`/ver/${ep.id}`)}
                                                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-primary hover:text-on-primary text-on-surface text-caption font-medium transition-all"
                                                        >
                                                            <span className="material-symbols-outlined text-[16px]">
                                                                play_circle
                                                            </span>
                                                            <span>Probar reproducción</span>
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => borrarEpisodio(ep.id, ep.numero)}
                                                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-error-container/25 hover:bg-error-container text-error hover:text-on-error-container border border-error/25 text-caption font-medium transition-all"
                                                        >
                                                            <span className="material-symbols-outlined text-[16px]">
                                                                delete
                                                            </span>
                                                            <span>Eliminar</span>
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                </div>
            )}

            {/* =====================================================================
                PESTAÑA 3: CUENTAS DE USUARIO
               ===================================================================== */}
            {tab === 'cuentas' && (
                <div>
                    {/* KPI Summary Cards */}
                    <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter mb-8">
                        <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl p-4 flex items-center justify-between">
                            <div>
                                <p className="text-label-sm uppercase tracking-wider text-outline">
                                    Cuentas Registradas
                                </p>
                                <p className="font-headline-xl text-2xl font-bold text-on-surface mt-1">
                                    {kpiUsuarios.total}
                                </p>
                            </div>
                            <div className="w-11 h-11 rounded-xl bg-primary/15 border border-primary/25 flex items-center justify-center text-primary">
                                <span className="material-symbols-outlined">group</span>
                            </div>
                        </div>

                        <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl p-4 flex items-center justify-between">
                            <div>
                                <p className="text-label-sm uppercase tracking-wider text-outline">
                                    Cuentas Premium
                                </p>
                                <p className="font-headline-xl text-2xl font-bold text-tertiary mt-1">
                                    {kpiUsuarios.premium}
                                </p>
                            </div>
                            <div className="w-11 h-11 rounded-xl bg-tertiary/15 border border-tertiary/25 flex items-center justify-center text-tertiary">
                                <span className="material-symbols-outlined">workspace_premium</span>
                            </div>
                        </div>

                        <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl p-4 flex items-center justify-between">
                            <div>
                                <p className="text-label-sm uppercase tracking-wider text-outline">
                                    Administradores
                                </p>
                                <p className="font-headline-xl text-2xl font-bold text-secondary mt-1">
                                    {kpiUsuarios.admins}
                                </p>
                            </div>
                            <div className="w-11 h-11 rounded-xl bg-secondary/15 border border-secondary/25 flex items-center justify-center text-secondary">
                                <span className="material-symbols-outlined">shield_person</span>
                            </div>
                        </div>

                        <div className="bg-surface-container-low border border-outline-variant/20 rounded-xl p-4 flex items-center justify-between">
                            <div>
                                <p className="text-label-sm uppercase tracking-wider text-outline">
                                    Perfiles Totales
                                </p>
                                <p className="font-headline-xl text-2xl font-bold text-emerald-400 mt-1">
                                    {kpiUsuarios.perfiles}
                                </p>
                            </div>
                            <div className="w-11 h-11 rounded-xl bg-emerald-500/15 border border-emerald-500/25 flex items-center justify-center text-emerald-400">
                                <span className="material-symbols-outlined">face</span>
                            </div>
                        </div>
                    </section>

                    {/* ACCOUNTS MANAGEMENT PANEL */}
                    <section className="bg-surface-container-low border border-outline-variant/25 rounded-2xl shadow-2xl overflow-hidden">
                        <div className="p-5 sm:p-6 border-b border-outline-variant/20 flex flex-col lg:flex-row lg:items-center justify-between gap-4 bg-surface-container/50">
                            <div>
                                <h2 className="font-title-card text-xl font-bold text-on-surface flex items-center gap-2.5">
                                    <span className="material-symbols-outlined text-primary">badge</span>
                                    Cuentas registradas ({usuariosFiltrados.length})
                                </h2>
                                <p className="text-caption text-on-surface-variant mt-0.5">
                                    Control de estados de cuenta, planes de suscripción y asignación de roles de seguridad.
                                </p>
                            </div>

                            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                                <div className="relative min-w-[260px]">
                                    <span className="material-symbols-outlined text-outline text-[18px] absolute left-3.5 top-1/2 -translate-y-1/2">
                                        search
                                    </span>
                                    <input
                                        type="text"
                                        value={busquedaUsuario}
                                        onChange={(e) => setBusquedaUsuario(e.target.value)}
                                        placeholder="Buscar por nombre o email..."
                                        className="w-full pl-10 pr-4 py-2 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-label-md text-on-surface placeholder:text-outline focus:outline-none focus:border-primary"
                                    />
                                </div>

                                <select
                                    value={filtroRol}
                                    onChange={(e) => setFiltroRol(e.target.value)}
                                    className="px-3.5 py-2 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-label-md text-on-surface-variant"
                                >
                                    <option value="">Todos los roles</option>
                                    <option value="ADMIN">Rol: ADMIN</option>
                                    <option value="USUARIO">Rol: USUARIO</option>
                                </select>

                                <select
                                    value={filtroPlan}
                                    onChange={(e) => setFiltroPlan(e.target.value)}
                                    className="px-3.5 py-2 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-label-md text-on-surface-variant"
                                >
                                    <option value="">Todos los planes</option>
                                    <option value="PREMIUM">Plan: Premium</option>
                                    <option value="GRATUITA">Plan: Estándar</option>
                                </select>
                            </div>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-surface-container-lowest/60 border-b border-outline-variant/25 text-label-sm uppercase tracking-wider text-outline">
                                        <th className="py-3.5 px-6 font-semibold">ID</th>
                                        <th className="py-3.5 px-4 font-semibold">Usuario</th>
                                        <th className="py-3.5 px-4 font-semibold">Estado</th>
                                        <th className="py-3.5 px-4 font-semibold">Suscripción</th>
                                        <th className="py-3.5 px-4 font-semibold">Roles</th>
                                        <th className="py-3.5 px-4 font-semibold text-center">Perfiles</th>
                                        <th className="py-3.5 px-6 font-semibold text-right">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/15 text-label-md">
                                    {usuariosFiltrados.map((u) => {
                                        const esAdminUser =
                                            u.roles?.includes('ADMIN') ||
                                            u.roles?.includes('ROLE_ADMIN')
                                        const esPremium = u.tipoSuscripcion === 'PREMIUM'
                                        const iniciales = (u.nombre || u.email || 'U')
                                            .split(' ')
                                            .map((p) => p[0])
                                            .join('')
                                            .slice(0, 2)
                                            .toUpperCase()

                                        return (
                                            <tr
                                                key={u.id}
                                                className="hover:bg-surface-container/60 transition-colors"
                                            >
                                                <td className="py-4 px-6 font-mono text-caption text-outline">
                                                    #{u.id}
                                                </td>
                                                <td className="py-4 px-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary-container to-secondary-container flex items-center justify-center text-on-primary font-bold text-label-sm shrink-0">
                                                            {iniciales}
                                                        </div>
                                                        <div>
                                                            <p className="font-semibold text-on-surface leading-tight">
                                                                {u.nombre}
                                                            </p>
                                                            <p className="text-caption text-on-surface-variant">
                                                                {u.email}
                                                            </p>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="py-4 px-4">
                                                    {u.activo ? (
                                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-label-sm bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                                                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                                                            Activo
                                                        </span>
                                                    ) : (
                                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-label-sm bg-error-container/30 text-error border border-error/30">
                                                            <span className="w-1.5 h-1.5 rounded-full bg-error" />
                                                            Bloqueado
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="py-4 px-4">
                                                    {esPremium ? (
                                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-label-sm bg-tertiary/15 text-tertiary border border-tertiary/30">
                                                            <span className="material-symbols-outlined text-[14px]">
                                                                workspace_premium
                                                            </span>
                                                            Premium
                                                        </span>
                                                    ) : (
                                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-label-sm bg-surface-container-highest text-on-surface-variant border border-outline-variant/30">
                                                            Estándar
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="py-4 px-4">
                                                    <div className="flex flex-wrap gap-1.5">
                                                        {(u.roles || []).map((r) => {
                                                            const limpio = r.replace('ROLE_', '')
                                                            return (
                                                                <span
                                                                    key={r}
                                                                    className={`px-2 py-0.5 rounded text-label-sm ${
                                                                        limpio === 'ADMIN'
                                                                            ? 'bg-primary/20 text-primary border border-primary/40'
                                                                            : 'bg-surface-container-high text-on-surface-variant border border-outline-variant/30'
                                                                    }`}
                                                                >
                                                                    {limpio}
                                                                </span>
                                                            )
                                                        })}
                                                    </div>
                                                </td>
                                                <td className="py-4 px-4 text-center">
                                                    <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-surface-container-high text-on-surface font-semibold text-caption">
                                                        {u.cantidadPerfiles}
                                                    </span>
                                                </td>
                                                <td className="py-4 px-6 text-right">
                                                    <div className="flex items-center justify-end gap-2 flex-wrap">
                                                        <button
                                                            type="button"
                                                            onClick={() => alternarActivoUsuario(u)}
                                                            className="px-2.5 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface text-label-sm border border-outline-variant/30 transition-colors"
                                                        >
                                                            {u.activo ? 'Bloquear' : 'Activar'}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => alternarRolAdmin(u)}
                                                            className="px-2.5 py-1.5 rounded-lg bg-secondary/15 hover:bg-secondary/25 text-secondary text-label-sm border border-secondary/30 transition-colors"
                                                        >
                                                            {esAdminUser ? 'Quitar Admin' : 'Hacer Admin'}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => alternarSuscripcion(u)}
                                                            className="px-2.5 py-1.5 rounded-lg bg-tertiary/15 hover:bg-tertiary/25 text-tertiary text-label-sm border border-tertiary/30 transition-colors"
                                                        >
                                                            {esPremium ? 'Quitar Premium' : 'Dar Premium'}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => borrarUsuario(u)}
                                                            className="px-2.5 py-1.5 rounded-lg bg-error-container/40 hover:bg-error-container/70 text-error text-label-sm border border-error/30 transition-colors inline-flex items-center gap-1"
                                                        >
                                                            <span className="material-symbols-outlined text-[14px]">
                                                                delete
                                                            </span>
                                                            Eliminar
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        )
                                    })}
                                </tbody>
                            </table>
                        </div>
                    </section>
                </div>
            )}
        </main>
    )
}
