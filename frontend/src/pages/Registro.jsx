import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Registro() {
    const { registrar } = useAuth()
    const navegar = useNavigate()

    const [form, setForm] = useState({
        email: '', password: '', nombre: '',
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
        <main className="centrado-vertical">
            <form className="formulario" onSubmit={enviar}>
                <h1>Crear cuenta</h1>

                {error && <div className="alerta alerta--error">{error}</div>}

                <div className="campo">
                    <label className="campo__etiqueta" htmlFor="nombre">Nombre</label>
                    <input id="nombre" className="campo__input" type="text" required
                           minLength={2} maxLength={150} autoComplete="name"
                           value={form.nombre}
                           onChange={(e) => actualizar('nombre', e.target.value)} />
                </div>

                <div className="campo">
                    <label className="campo__etiqueta" htmlFor="email">Email</label>
                    <input id="email" className="campo__input" type="email" required
                           autoComplete="email" value={form.email}
                           onChange={(e) => actualizar('email', e.target.value)} />
                </div>

                <div className="campo">
                    <label className="campo__etiqueta" htmlFor="password">Contraseña</label>
                    <input id="password" className="campo__input" type="password" required
                           minLength={10} maxLength={72} autoComplete="new-password"
                           value={form.password}
                           onChange={(e) => actualizar('password', e.target.value)} />
                    <p className="campo__ayuda">
                        Mínimo 10 caracteres, con al menos una mayúscula, una minúscula y un número.
                    </p>
                </div>

                {/* ==========================================================
            LEY 19.628 - CONSENTIMIENTO EXPLICITO Y SEPARADO
            Dos casillas distintas, no una. La norma exige
            consentimiento especifico por cada finalidad del
            tratamiento; agrupar ambas en un solo checkbox es
            precisamente lo que busca evitar.
            El backend persiste cada respuesta con su version de
            terminos, fecha e IP en la tabla consentimiento.
           ========================================================== */}

                <div className="consentimiento consentimiento--obligatorio">
                    <input id="terminos" type="checkbox" required
                           checked={form.aceptaTerminos}
                           onChange={(e) => actualizar('aceptaTerminos', e.target.checked)} />
                    <label htmlFor="terminos">
                        Acepto los <a href="/terminos" target="_blank" rel="noreferrer">términos y
                        condiciones</a> y autorizo el tratamiento de mis datos personales conforme
                        a la Ley N° 19.628. <strong>(Obligatorio)</strong>
                    </label>
                </div>

                <div className="consentimiento">
                    <input id="recomendaciones" type="checkbox"
                           checked={form.aceptaRecomendaciones}
                           onChange={(e) => actualizar('aceptaRecomendaciones', e.target.checked)} />
                    <label htmlFor="recomendaciones">
                        Autorizo el uso de mi historial de visualización para generar
                        recomendaciones personalizadas. <em>(Opcional — puedes registrarte
                        sin aceptarlo)</em>
                    </label>
                </div>

                <button className="boton boton--bloque mt-4" type="submit"
                        disabled={enviando || !form.aceptaTerminos}>
                    {enviando ? 'Creando cuenta...' : 'Crear cuenta'}
                </button>

                <p className="texto-tenue mt-4" style={{ textAlign: 'center' }}>
                    ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
                </p>
            </form>
        </main>
    )
}
