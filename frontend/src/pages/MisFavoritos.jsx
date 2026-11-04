import { useEffect, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { usePerfil } from '../context/PerfilContext'
import { interaccionesApi } from '../api/interacciones'
import AnimeCard from '../components/AnimeCard'
import Cargando from '../components/Cargando'

export default function MisFavoritos() {
    const { perfilActivo } = usePerfil()
    const [pagina, setPagina] = useState(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState(null)
    const [busqueda, setBusqueda] = useState('')
    const [orden, setOrden] = useState('recientes')

    useEffect(() => {
        if (!perfilActivo) return
        let activo = true
        setCargando(true)
        interaccionesApi
            .listarFavoritos(perfilActivo.id)
            .then((datos) => {
                if (activo) setPagina(datos)
            })
            .catch((ex) => {
                if (activo) setError(ex.message)
            })
            .finally(() => {
                if (activo) setCargando(false)
            })
        return () => {
            activo = false
        }
    }, [perfilActivo?.id])

    const favoritos = pagina?.content || pagina?.contenido || (Array.isArray(pagina) ? pagina : [])
    const incompletos = favoritos.filter((f) => f.datosCompletos === false).length

    async function quitarDeFavoritos(animeId) {
        if (!perfilActivo) return
        try {
            await interaccionesApi.quitarFavorito(perfilActivo.id, animeId)
            setPagina((prev) => {
                const listaActual = prev?.content || prev?.contenido || []
                return {
                    ...prev,
                    content: listaActual.filter((f) => (f.animeId ?? f.id) !== animeId),
                }
            })
        } catch (ex) {
            setError(ex.message)
        }
    }

    const favoritosFiltrados = useMemo(() => {
        const q = busqueda.trim().toLowerCase()
        const filtrados = favoritos.filter((f) => {
            const titulo = (f.titulo || f.animeTitulo || '').toLowerCase()
            return !q || titulo.includes(q)
        })
        if (orden === 'az') {
            return [...filtrados].sort((a, b) =>
                (a.titulo || a.animeTitulo || '').localeCompare(b.titulo || b.animeTitulo || '')
            )
        }
        return filtrados
    }, [favoritos, busqueda, orden])

    if (cargando) return <Cargando />

    return (
        <main className="relative pt-24 pb-20 min-h-screen overflow-hidden">
            {/* Ambient Stage Lighting Glows */}
            <div className="pointer-events-none fixed top-0 left-1/4 w-[600px] h-[400px] bg-primary-container/10 blur-[140px] rounded-full -z-10" />
            <div className="pointer-events-none fixed bottom-0 right-1/4 w-[500px] h-[350px] bg-secondary-container/10 blur-[130px] rounded-full -z-10" />

            <div className="max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop">
                {/* Hero Profile & Collection Header */}
                <section className="relative rounded-2xl bg-surface-container-low border border-outline-variant/25 p-6 md:p-8 mb-8 overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.45)]">
                    <div className="pointer-events-none absolute -top-24 -right-24 w-96 h-96 bg-gradient-to-br from-primary/15 via-primary-container/10 to-transparent blur-3xl rounded-full" />

                    <div className="relative z-10 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
                        <div className="space-y-3 max-w-2xl">
                            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/25 text-primary text-label-sm">
                                <span
                                    className="material-symbols-outlined text-[15px]"
                                    style={{ fontVariationSettings: "'FILL' 1" }}
                                >
                                    favorite
                                </span>
                                <span>Colección Personal · {perfilActivo?.nombre}</span>
                            </div>
                            <h1 className="font-headline-xl text-3xl md:text-4xl font-extrabold text-on-surface tracking-tight">
                                Favoritos de <span className="text-primary">{perfilActivo?.nombre}</span>
                            </h1>
                            <p className="text-body-md text-on-surface-variant leading-relaxed">
                                Tus series guardadas alimentan directamente el motor de recomendaciones de{' '}
                                <span className="text-on-surface font-medium">SugoiAnime</span>. Mientras más títulos agregues, más precisas serán tus sugerencias personalizadas.
                            </p>
                        </div>

                        {/* Collection Stats & Quick CTA */}
                        <div className="flex flex-wrap sm:flex-nowrap items-center gap-4">
                            <div className="flex items-center gap-4 px-5 py-3.5 rounded-xl bg-surface-container border border-outline-variant/25">
                                <div className="text-center pr-4 border-r border-outline-variant/25">
                                    <span className="block font-headline-xl text-2xl font-extrabold text-primary">
                                        {favoritos.length}
                                    </span>
                                    <span className="text-label-sm text-on-surface-variant uppercase tracking-wider">
                                        Guardados
                                    </span>
                                </div>
                                <div className="text-center">
                                    <div className="flex items-center justify-center gap-1">
                                        <span
                                            className="material-symbols-outlined text-tertiary text-lg"
                                            style={{ fontVariationSettings: "'FILL' 1" }}
                                        >
                                            auto_awesome
                                        </span>
                                        <span className="font-headline-xl text-xl font-extrabold text-on-surface">
                                            TF-IDF
                                        </span>
                                    </div>
                                    <span className="text-label-sm text-on-surface-variant uppercase tracking-wider">
                                        Motor Activo
                                    </span>
                                </div>
                            </div>

                            <Link
                                to="/catalogo"
                                className="inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-xl bg-gradient-to-r from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_0_24px_rgba(160,120,255,0.3)] hover:shadow-[0_0_32px_rgba(160,120,255,0.5)] hover:scale-[1.01] transition-all whitespace-nowrap"
                            >
                                <span className="material-symbols-outlined text-[20px]">explore</span>
                                <span>Explorar el catálogo</span>
                            </Link>
                        </div>
                    </div>

                    {/* Recommendation Engine Tip Box */}
                    <div className="relative z-10 mt-6 pt-5 border-t border-outline-variant/20 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div className="flex items-center gap-3 text-caption text-on-surface-variant">
                            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-secondary-container/20 text-secondary border border-secondary/20">
                                <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                            </span>
                            <span>
                                <strong className="text-on-surface font-medium">Algoritmo Sugoi Engine:</strong>{' '}
                                Tus favoritos actuales recalibran las sugerencias de la pantalla de{' '}
                                <Link to="/" className="text-primary hover:underline font-medium">
                                    Inicio
                                </Link>{' '}
                                en tiempo real.
                            </span>
                        </div>
                    </div>
                </section>

                {error && (
                    <div className="mb-6 p-4 rounded-xl bg-error-container/30 border border-error/40 text-error text-label-md">
                        {error}
                    </div>
                )}

                {incompletos > 0 && (
                    <div className="mb-6 p-4 rounded-xl bg-surface-container border border-outline-variant/30 text-on-surface-variant text-caption">
                        No se pudieron cargar los detalles de {incompletos} título{incompletos !== 1 ? 's' : ''}. El servicio de catálogo podría estar temporalmente no disponible.
                    </div>
                )}

                {/* Filter & Search Bar */}
                {favoritos.length > 0 && (
                    <section className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 mb-8">
                        <div className="relative flex-1 max-w-md">
                            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-outline text-[20px]">
                                search
                            </span>
                            <input
                                type="text"
                                value={busqueda}
                                onChange={(e) => setBusqueda(e.target.value)}
                                placeholder="Filtrar en mis favoritos..."
                                className="w-full pl-11 pr-4 py-2.5 rounded-xl bg-surface-container-low border border-outline-variant/30 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/40 transition-all"
                            />
                        </div>

                        <div className="flex items-center gap-2">
                            <label className="text-caption text-on-surface-variant whitespace-nowrap" htmlFor="sort-favs">
                                Ordenar por:
                            </label>
                            <select
                                id="sort-favs"
                                value={orden}
                                onChange={(e) => setOrden(e.target.value)}
                                className="rounded-xl bg-surface-container-low border border-outline-variant/30 text-on-surface text-label-md py-2 pl-3.5 pr-8 focus:outline-none focus:border-primary transition-all"
                            >
                                <option value="recientes">Agregados recientemente</option>
                                <option value="az">Título (A - Z)</option>
                            </select>
                        </div>
                    </section>
                )}

                {/* Favorites Grid or Empty State */}
                {favoritos.length === 0 ? (
                    <section className="mt-8 rounded-2xl bg-surface-container-low/60 border border-dashed border-outline-variant/40 p-10 text-center max-w-2xl mx-auto">
                        <div className="w-16 h-16 rounded-2xl bg-primary/10 border border-primary/25 flex items-center justify-center mx-auto mb-4 text-primary">
                            <span className="material-symbols-outlined text-3xl">favorite_border</span>
                        </div>
                        <h3 className="font-title-card text-xl font-bold text-on-surface mb-2">
                            Aún no tienes favoritos en este perfil
                        </h3>
                        <p className="text-body-md text-on-surface-variant mb-6 text-sm">
                            Explora el{' '}
                            <Link to="/catalogo" className="text-primary underline">
                                catálogo
                            </Link>{' '}
                            y marca los animes que te gusten para activar tus recomendaciones personalizadas.
                        </p>
                        <Link
                            to="/catalogo"
                            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-surface-container-high hover:bg-surface-bright text-primary font-semibold text-label-md border border-primary/30 transition-all"
                        >
                            <span className="material-symbols-outlined text-[18px]">add_circle</span>
                            <span>Explorar el catálogo</span>
                        </Link>
                    </section>
                ) : (
                    <section
                        aria-label="Lista de animes favoritos"
                        className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-gutter lg:gap-gutter-desktop"
                    >
                        {favoritosFiltrados.map((favorito) => {
                            const animeId = favorito.animeId ?? favorito.id
                            return (
                                <AnimeCard
                                    key={favorito.favoritoId ?? animeId}
                                    anime={favorito}
                                    etiqueta="Favorito"
                                    accionSuperiorDerecha={
                                        <button
                                            type="button"
                                            onClick={() => quitarDeFavoritos(animeId)}
                                            title="Quitar de favoritos"
                                            className="w-8 h-8 rounded-full bg-surface-container-lowest/80 backdrop-blur-md text-primary hover:text-error hover:bg-error-container/30 flex items-center justify-center border border-primary/30 transition-all shadow-lg"
                                        >
                                            <span
                                                className="material-symbols-outlined text-[18px]"
                                                style={{ fontVariationSettings: "'FILL' 1" }}
                                            >
                                                favorite
                                            </span>
                                        </button>
                                    }
                                />
                            )
                        })}
                    </section>
                )}
            </div>
        </main>
    )
}
