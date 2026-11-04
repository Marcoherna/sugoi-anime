import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Login() {
    const { login } = useAuth()
    const navegar = useNavigate()
    const ubicacion = useLocation()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState(null)
    const [enviando, setEnviando] = useState(false)

    async function enviar(evento) {
        evento.preventDefault()
        setError(null)
        setEnviando(true)
        try {
            await login(email, password)
            navegar(ubicacion.state?.destino || '/perfiles')
        } catch (ex) {
            setError(ex.message)
        } finally {
            setEnviando(false)
        }
    }

    return (
        <main className="flex-1 flex items-center justify-center pt-24 pb-16 px-4 relative overflow-hidden">
            {/* Ambient Stage Glow */}
            <div className="pointer-events-none fixed top-1/4 left-1/2 -translate-x-1/2 w-[520px] h-[360px] bg-primary-container/15 blur-[140px] rounded-full -z-10" />

            <form
                onSubmit={enviar}
                className="w-full max-w-md rounded-2xl bg-surface-container-low/95 backdrop-blur-xl border border-outline-variant/30 p-7 sm:p-9 shadow-[0_24px_60px_rgba(0,0,0,0.65)] relative overflow-hidden space-y-5"
            >
                <div className="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-primary/50 to-transparent" />

                <div className="text-center space-y-2">
                    <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary to-primary-container flex items-center justify-center mx-auto shadow-[0_0_20px_rgba(160,120,255,0.4)]">
                        <span className="material-symbols-outlined text-on-primary text-2xl font-bold">bolt</span>
                    </div>
                    <h1 className="font-headline-xl text-2xl font-extrabold text-on-surface">
                        Iniciar sesión
                    </h1>
                    <p className="text-caption text-on-surface-variant">
                        Accede a tu cuenta de SugoiAnime para continuar viendo
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
                        htmlFor="email"
                    >
                        Email
                    </label>
                    <input
                        id="email"
                        type="email"
                        required
                        autoComplete="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="tu@correo.cl"
                        className="w-full px-4 py-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
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
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••••"
                        className="w-full px-4 py-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40 text-on-surface placeholder-outline text-label-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                    />
                </div>

                <button
                    type="submit"
                    disabled={enviando}
                    className="w-full py-3.5 rounded-xl bg-gradient-to-br from-primary to-primary-container text-on-primary font-bold text-label-md shadow-[0_8px_24px_rgba(160,120,255,0.35)] hover:shadow-[0_12px_32px_rgba(160,120,255,0.5)] disabled:opacity-40 transition-all"
                >
                    {enviando ? 'Ingresando...' : 'Ingresar'}
                </button>

                <p className="text-caption text-on-surface-variant text-center pt-2 border-t border-outline-variant/20">
                    ¿No tienes cuenta?{' '}
                    <Link to="/registro" className="text-primary font-semibold hover:underline">
                        Regístrate
                    </Link>
                </p>
            </form>
        </main>
    )
}
