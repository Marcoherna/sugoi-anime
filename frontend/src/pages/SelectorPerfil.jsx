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
      <main className="centrado-vertical">
        <div style={{ width: '100%', maxWidth: 640, textAlign: 'center' }}>
          <h1>¿Quién está viendo?</h1>
          <p className="texto-suave mb-6">
            Cada perfil tiene sus propios favoritos, historial y recomendaciones.
          </p>

          {error && <div className="alerta alerta--error">{error}</div>}

          <div className="perfiles mb-6">
            {perfiles.map((perfil) => (
                <button key={perfil.id} className="perfil" onClick={() => elegir(perfil)}>
              <span className={`perfil__avatar${perfil.esInfantil ? ' perfil__avatar--infantil' : ''}`}>
                {perfil.nombre.charAt(0).toUpperCase()}
              </span>
                  <span className="perfil__nombre">{perfil.nombre}</span>
                  {perfil.esInfantil && <span className="perfil__etiqueta">Infantil</span>}
                  {perfiles.length > 1 && (
                      <span className="texto-tenue" style={{ fontSize: '0.72rem' }}
                            onClick={(e) => borrar(e, perfil.id)}>
                  Eliminar
                </span>
                  )}
                </button>
            ))}

            {perfiles.length < 5 && !creando && (
                <button className="perfil" onClick={() => setCreando(true)}>
              <span className="perfil__avatar"
                    style={{ background: 'var(--fondo-tarjeta)',
                      border: '2px dashed var(--borde)', color: 'var(--texto-suave)' }}>
                +
              </span>
                  <span className="perfil__nombre">Agregar perfil</span>
                </button>
            )}
          </div>

          {creando && (
              <form className="formulario" style={{ marginInline: 'auto' }} onSubmit={crearPerfil}>
                <h2>Nuevo perfil</h2>
                <div className="campo">
                  <label className="campo__etiqueta" htmlFor="perfil-nombre">Nombre</label>
                  <input id="perfil-nombre" className="campo__input" type="text" required
                         maxLength={50} value={nombre}
                         onChange={(e) => setNombre(e.target.value)} />
                </div>
                <div className="consentimiento">
                  <input id="infantil" type="checkbox" checked={esInfantil}
                         onChange={(e) => setEsInfantil(e.target.checked)} />
                  <label htmlFor="infantil">
                    Perfil infantil — se aplicarán restricciones de contenido por edad
                  </label>
                </div>
                <div className="flex-fila mt-4">
                  <button className="boton" type="submit">Crear</button>
                  <button className="boton boton--secundario" type="button"
                          onClick={() => setCreando(false)}>Cancelar</button>
                </div>
              </form>
          )}

          {perfiles.length >= 5 && (
              <p className="texto-tenue">Alcanzaste el máximo de 5 perfiles por cuenta.</p>
          )}
        </div>
      </main>
  )
}
