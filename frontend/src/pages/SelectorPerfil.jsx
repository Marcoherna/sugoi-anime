import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { usePerfil } from '../context/PerfilContext'
import Cargando from '../components/Cargando'

export default function SelectorPerfil() {
    const { perfiles, cargando, seleccionar, crear, eliminar } = usePerfil()
    const navegar = useNavigate()
    const ubicacion = useLocation()

    const [creando, setCreando] = useState(false)
    const [nombre, setNombre] = useState('')
    const [esInfantil, setEsInfantil] = useState(false)
    const [error, setError] = useState(null)

    function elegir(perfil) {
        seleccionar(perfil)
        navegar(ubicacion.state?.destino || '/')
    }

    async function crearPerfil(evento) {
        evento.preventDefault()
        setError(null)
        try {
            const nuevo = await crear({ nombre, esInfantil, avatar: 'default' })
            setNombre('')
            setEsInfantil(false)
            setCreando(false)
            elegir(nuevo)
        } catch (ex) {
            setError(ex.message)
        }
    }

    async function borrar(evento, id) {
        evento.stopPropagation()
        if (!confirm('¿Eliminar este perfil? Se perderán sus favoritos y su historial.')) return
        try {
            await eliminar(id)
        } catch (ex) {
            setError(ex.message)
        }
    }

    if (cargando) return <Cargando texto="Cargando perfiles..." />

    return (
        <main className="flex-1 flex items-center justify-center pt-24 pb-16 px-4 relative overflow-hidden">
            <div className="pointer-events-none fixed top-1/4 left-1/2 -translate-x-1/2 w-[640px] h-[380px] bg-primary-container/15 blur-[140px] rounded-full -z-10" />

            <div className="w-full max-w-3xl text-center space-y-8">
                <div className="space-y-2">
                    <span className="text-label-sm uppercase tracking-widest text-primary">
                        Experiencia Multi-Perfil
                    </span>
                    <h1 className="font-headline-xl text-3xl sm:text-4xl font-extrabold text-on-surface">
                        ¿Quién está viendo?
                    </h1>
                    <p className="text-body-md text-on-surface-variant">
                        Cada perfil tiene sus propios favoritos, historial de reproducción y recomendaciones.
                    </p>
                </div>

                {error && (
                    <div className="p-4 rounded-xl bg-error-container/30 border border-error/40 text-error text-caption max-w-md mx-auto">
                        {error}
                    </div>
                )}

                <div className="flex flex-wrap items-center justify-center gap-6">
                    {perfiles.map((perfil) => (
                        <div
                            key={perfil.id}
                            onClick={() => elegir(perfil)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(e) => e.key === 'Enter' && elegir(perfil)}
                            className="group flex flex-col items-center gap-3 p-5 rounded-2xl bg-surface-container-low hover:bg-surface-container-high border border-outline-variant/25 hover:border-primary/50 w-40 cursor-pointer transition-all hover:-translate-y-1.5 shadow-lg"
                        >
                            <div
                                className={`w-20 h-20 rounded-full flex items-center justify-center font-headline-xl text-3xl font-extrabold text-on-primary shadow-lg group-hover:scale-105 transition-transform ${
                                    perfil.esInfantil
                                        ? 'bg-gradient-to-br from-tertiary to-tertiary-container'
                                        : 'bg-gradient-to-br from-primary to-primary-container'
                                }`}
                            >
                                {perfil.nombre.charAt(0).toUpperCase()}
                            </div>
                            <span className="font-title-card text-base font-bold text-on-surface group-hover:text-primary transition-colors truncate max-w-full">
                                {perfil.nombre}
                            </span>
                            {perfil.esInfantil && (
                                <span className="px-2 py-0.5 rounded bg-tertiary/15 text-tertiary text-[10px] font-bold uppercase tracking-wider">
                                    Infantil
                                </span>
                            )}
                            {perfiles.length > 1 && (
                                <button
                                    type="button"
                                    onClick={(e) => borrar(e, perfil.id)}
                                    className="text-[11px] text-outline hover:text-error transition-colors"
                                >
                                    Eliminar
                                </button>
                            )}
                        </div>
                    ))}

                    {perfiles.length < 5 && !creando && (
                        <button
                            type="button"
                            onClick={() => setCreando(true)}
                            className="group flex flex-col items-center justify-center gap-3 p-5 rounded-2xl bg-surface-container-low/60 hover:bg-surface-container-high border-2 border-dashed border-outline-variant/40 hover:border-primary/50 w-40 h-[196px] transition-all hover:-translate-y-1.5"
                        >
                            <div className="w-16 h-16 rounded-full bg-surface-container-highest flex items-center justify-center text-outline group-hover:text-primary transition-colors">
                                <span className="material-symbols-outlined text-3xl">add</span>
                            </div>
                            <span className="text-label-md font-semibold text-on-surface-variant group-hover:text-on-surface">
                                Agregar perfil
                            </span>
                        </button>
                    )}
                </div>

                {creando && (
                    <form
                        onSubmit={crearPerfil}
                        className="max-w-md mx-auto text-left rounded-2xl bg-surface-container-low border border-outline-variant/30 p-6 shadow-2xl space-y-4"
                    >
                        <h2 className="font-title-card text-lg font-bold text-on-surface">Nuevo perfil</h2>
                        <div className="space-y-1.5">
                            <label
                                className="block text-label-sm uppercase tracking-wider text-on-surface-variant"
                                htmlFor="perfil-nombre"
                            >
                                Nombre
                            </label>
                            <input
                                id="perfil-nombre"
                                type="text"
                                required
                                maxLength={50}
                                value={nombre}
                                onChange={(e) => setNombre(e.target.value)}
                                placeholder="Ej: Akira"
                                className="w-full px-4 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface text-label-md focus:outline-none focus:border-primary"
                            />
                        </div>
                        <div className="flex items-start gap-2.5 p-3 rounded-xl bg-surface-container border border-outline-variant/20">
                            <input
                                id="infantil"
                                type="checkbox"
                                checked={esInfantil}
                                onChange={(e) => setEsInfantil(e.target.checked)}
                                className="mt-1 rounded border-outline-variant text-primary"
                            />
                            <label htmlFor="infantil" className="text-caption text-on-surface-variant">
                                Perfil infantil — se aplicarán restricciones de contenido por edad
                            </label>
                        </div>
                        <div className="flex items-center gap-3 pt-2">
                            <button
                                type="submit"
                                className="px-5 py-2.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md"
                            >
                                Crear
                            </button>
                            <button
                                type="button"
                                onClick={() => setCreando(false)}
                                className="px-4 py-2.5 rounded-xl bg-surface-container-high text-on-surface text-label-md"
                            >
                                Cancelar
                            </button>
                        </div>
                    </form>
                )}

                {perfiles.length >= 5 && (
                    <p className="text-caption text-outline">
                        Alcanzaste el máximo de 5 perfiles por cuenta.
                    </p>
                )}
            </div>
        </main>
    )
}
