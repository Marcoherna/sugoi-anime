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
            // Vuelve al destino original si venia de una ruta protegida
            navegar(ubicacion.state?.destino || '/perfiles')
        } catch (ex) {
            setError(ex.message)
        } finally {
            setEnviando(false)
        }
    }

    return (
        <main className="centrado-vertical">
            <form className="formulario" onSubmit={enviar}>
                <h1>Iniciar sesión</h1>

                {error && <div className="alerta alerta--error">{error}</div>}

                <div className="campo">
                    <label className="campo__etiqueta" htmlFor="email">Email</label>
                    <input id="email" className="campo__input" type="email" required
                           autoComplete="email" value={email}
                           onChange={(e) => setEmail(e.target.value)} />
                </div>

                <div className="campo">
                    <label className="campo__etiqueta" htmlFor="password">Contraseña</label>
                    <input id="password" className="campo__input" type="password" required
                           autoComplete="current-password" value={password}
                           onChange={(e) => setPassword(e.target.value)} />
                </div>

                <button className="boton boton--bloque mt-4" type="submit" disabled={enviando}>
                    {enviando ? 'Ingresando...' : 'Ingresar'}
                </button>

                <p className="texto-tenue mt-4" style={{ textAlign: 'center' }}>
                    ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
                </p>
            </form>
        </main>
    )
}
