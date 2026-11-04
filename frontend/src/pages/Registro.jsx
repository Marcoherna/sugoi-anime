import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Registro() {
    const { registrar } = useAuth()
    const navegar = useNavigate()

    const [form, setForm] = useState({
        email: '',
        password: '',
        nombre: '',
        aceptaTerminos: false,
        aceptaRecomendaciones: false,
    })
    const [error, setError] = useState(null)
    const [enviando, setEnviando] = useState(false)

    function actualizar(campo, valor) {
        setForm((prev) => ({ ...prev, [campo]: valor }))
    }

    async function enviar(evento) {
        evento.preventDefault()
        setError(null)
        setEnviando(true)
        try {
            await registrar(form)
            navegar('/perfiles')
        } catch (ex) {
            setError(ex.cuerpo?.detalles?.join('. ') || ex.message)
        } finally {
            setEnviando(false)
        }
    }

    return (
        <main className="flex-1 flex items-center justify-center pt-24 pb-16 px-4 relative overflow-hidden">
            <div className="pointer-events-none fixed top-1/4 left-1/2 -translate-x-1/2 w-[550px] h-[380px] bg-primary-container/15 blur-[140px] rounded-full -z-10" />

            <form
                onSubmit={enviar}
                className="w-full max-w-lg rounded-2xl bg-surface-container-low/95 backdrop-blur-xl border border-outline-variant/30 p-7 sm:p-9 shadow-[0_24px_60px_rgba(0,0,0,0.65)] relative overflow-hidden space-y-5"
            >
                <div className="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-primary/50 to-transparent" />

                <div className="text-center space-y-2">
                    <img
                        src="/logo.png"
                        alt="SugoiAnime"
                        className="w-16 h-16 rounded-2xl object-contain mx-auto shadow-[0_0_24px_rgba(160,120,255,0.45)]"
                    />
                    <h1 className="font-headline-xl text-2xl font-extrabold text-on-surface">
                        Crear cuenta en SugoiAnime
                    </h1>
                    <p className="text-caption text-on-surface-variant">
                        Configura tu cuenta y tus preferencias de privacidad (Ley 19.628)
                    </p>
                </div>

                {error && (
                    <div className="p-3.5 rounded-xl bg-error-container/30 border border-error/40 text-error text-caption flex items-center gap-2">
                        <span className="material-symbols-outlined text-base">error</span>
                        <span>{error}</span>
                    </div>
                )}

                <div className="space-y-1.5">
                    <label
                        className="block text-label-sm uppercase tracking-wider text-on-surface-variant"
                        htmlFor="nombre"
                    >
                        Nombre
                    </label>
                    <input
                        id="nombre"
                        type="text"
                        required
                        minLength={2}
                        maxLength={150}
                        autoComplete="name"
                        value={form.nombre}
                        onChange={(e) => actualizar('nombre', e.target.value)}
                        placeholder="Tu nombre"
                        className="w-full px-4 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                    />
                </div>

                <div className="space-y-1.5">
                    <label
                        className="block text-label-sm uppercase tracking-wider text-on-surface-variant"
                        htmlFor="email"
                    >
                        Email
                    </label>
                    <input
                        id="email"
                        type="email"
                        required
                        autoComplete="email"
                        value={form.email}
                        onChange={(e) => actualizar('email', e.target.value)}
                        placeholder="tu@correo.cl"
                        className="w-full px-4 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                    />
                </div>

                <div className="space-y-1.5">
                    <label
                        className="block text-label-sm uppercase tracking-wider text-on-surface-variant"
                        htmlFor="password"
                    >
                        Contraseña
                    </label>
                    <input
                        id="password"
                        type="password"
                        required
                        minLength={10}
                        maxLength={72}
                        autoComplete="new-password"
                        value={form.password}
                        onChange={(e) => actualizar('password', e.target.value)}
                        placeholder="Mínimo 10 caracteres"
                        className="w-full px-4 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                    />
                    <p className="text-[11px] text-outline">
                        Mínimo 10 caracteres, con al menos una mayúscula, una minúscula y un número.
                    </p>
                </div>

                {/* LEY 19.628 - CONSENTIMIENTO EXPLÍCITO Y SEPARADO */}
                <div className="space-y-3 pt-1">
                    <div className="flex items-start gap-3 p-3.5 rounded-xl bg-surface-container border border-primary/30">
                        <input
                            id="terminos"
                            type="checkbox"
                            required
                            checked={form.aceptaTerminos}
                            onChange={(e) => actualizar('aceptaTerminos', e.target.checked)}
                            className="mt-1 rounded border-outline-variant text-primary focus:ring-primary"
                        />
                        <label htmlFor="terminos" className="text-caption text-on-surface-variant leading-snug">
                            Acepto los{' '}
                            <a href="/terminos" target="_blank" rel="noreferrer" className="text-primary underline">
                                términos y condiciones
                            </a>{' '}
                            y autorizo el tratamiento de mis datos personales conforme a la Ley N° 19.628.{' '}
                            <strong className="text-on-surface">(Obligatorio)</strong>
                        </label>
                    </div>

                    <div className="flex items-start gap-3 p-3.5 rounded-xl bg-surface-container border border-outline-variant/25">
                        <input
                            id="recomendaciones"
                            type="checkbox"
                            checked={form.aceptaRecomendaciones}
                            onChange={(e) => actualizar('aceptaRecomendaciones', e.target.checked)}
                            className="mt-1 rounded border-outline-variant text-primary focus:ring-primary"
                        />
                        <label
                            htmlFor="recomendaciones"
                            className="text-caption text-on-surface-variant leading-snug"
                        >
                            Autorizo el uso de mi historial de visualización para generar recomendaciones personalizadas.{' '}
                            <em className="text-outline">(Opcional — puedes registrarte sin aceptarlo)</em>
                        </label>
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={enviando || !form.aceptaTerminos}
                    className="w-full py-3.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_8px_24px_rgba(160,120,255,0.35)] hover:shadow-[0_12px_32px_rgba(160,120,255,0.5)] disabled:opacity-40 transition-all"
                >
                    {enviando ? 'Creando cuenta...' : 'Crear cuenta'}
                </button>

                <p className="text-caption text-on-surface-variant text-center pt-2 border-t border-outline-variant/20">
                    ¿Ya tienes cuenta?{' '}
                    <Link to="/login" className="text-primary font-semibold hover:underline">
                        Inicia sesión
                    </Link>
                </p>
            </form>
        </main>
    )
}
