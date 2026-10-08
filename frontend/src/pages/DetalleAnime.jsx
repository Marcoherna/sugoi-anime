import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import { interaccionesApi } from '../api/interacciones'
import { usePerfil } from '../context/PerfilContext'
import { useAuth } from '../context/AuthContext'
import Cargando from '../components/Cargando'

export default function DetalleAnime() {
  const { id } = useParams()
  const navegar = useNavigate()
  const { autenticado } = useAuth()
  const { perfilActivo } = usePerfil()

  const [anime, setAnime] = useState(null)
  const [progresos, setProgresos] = useState({})
  const [esFavorito, setEsFavorito] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    setCargando(true)

    catalogoApi.detalle(id)
        .then((datos) => { if (activo) setAnime(datos) })
        .catch((ex) => { if (activo) setError(ex.message) })
        .finally(() => { if (activo) setCargando(false) })

    return () => { activo = false }
  }, [id])

  useEffect(() => {
    if (!perfilActivo) return
    let activo = true

    interaccionesApi.estadoFavorito(perfilActivo.id, id)
        .then((r) => { if (activo) setEsFavorito(r.esFavorito) })
        .catch(() => {})

    interaccionesApi.progresoAnime(perfilActivo.id, id)
        .then((lista) => {
          if (!activo) return
          const mapa = {}
          lista.forEach((p) => { mapa[p.episodioId] = p })
          setProgresos(mapa)
        })
        .catch(() => {})

    return () => { activo = false }
  }, [perfilActivo, id])

  async function alternarFavorito() {
    if (!perfilActivo) { navegar('/perfiles'); return }
    try {
      if (esFavorito) {
        await interaccionesApi.quitarFavorito(perfilActivo.id, id)
        setEsFavorito(false)
      } else {
        await interaccionesApi.agregarFavorito(perfilActivo.id, id)
        setEsFavorito(true)
      }
    } catch (ex) {
      setError(ex.message)
    }
  }

  if (cargando) return <Cargando />
  if (error) return <main className="contenedor pagina">
    <div className="alerta alerta--error">{error}</div></main>
  if (!anime) return null

  return (
      <main className="contenedor pagina">
        <div className="detalle">
          <div>
            {anime.imagenUrl
                ? <img className="detalle__portada" src={anime.imagenUrl} alt={anime.titulo} />
                : <div className="detalle__portada" style={{ background: 'var(--fondo-tarjeta)' }} />}
          </div>

          <div>
            <h1>{anime.titulo}</h1>
            {anime.tituloJapones && (
                <p className="texto-tenue" style={{ marginTop: '-0.75rem' }}>
                  {anime.tituloJapones}
                </p>
            )}

            <div className="flex-fila mb-6">
              {anime.puntuacion && (
                  <span className="tarjeta__puntaje">★ {Number(anime.puntuacion).toFixed(2)}</span>
              )}
              {anime.anio && <span className="texto-suave">{anime.anio}</span>}
              {anime.episodios && (
                  <span className="texto-suave">{anime.episodios} episodios</span>
              )}
              <span className="etiqueta">{anime.estado}</span>
            </div>

            <div className="etiquetas">
              {anime.generos?.map((g) => (
                  <span key={g.id} className="etiqueta">{g.nombre}</span>
              ))}
            </div>

            <p className="texto-suave">{anime.sinopsis || 'Sin sinopsis disponible.'}</p>
            {anime.sinopsisTraducida && (
                <p className="texto-tenue">Sinopsis traducida automáticamente al español.</p>
            )}

            <div className="flex-fila mb-6">
              {autenticado ? (
                  <button className={`boton${esFavorito ? ' boton--secundario' : ''}`}
                          onClick={alternarFavorito}>
                    {esFavorito ? '♥ En favoritos' : '♡ Agregar a favoritos'}
                  </button>
              ) : (
                  <Link to="/login" className="boton boton--secundario">
                    Inicia sesión para guardar
                  </Link>
              )}
            </div>

            <h2>Episodios disponibles</h2>
            {anime.episodiosDisponibles?.length > 0 ? (
                <div className="episodios">
                  {anime.episodiosDisponibles.map((ep) => {
                    const progreso = progresos[ep.id]
                    return (
                        <button key={ep.id} className="episodio"
                                onClick={() => navegar(`/ver/${ep.id}`)}>
                    <span>
                      <strong>Episodio {ep.numero}</strong>
                      {ep.titulo && <span className="texto-suave"> — {ep.titulo}</span>}
                      {progreso?.porcentaje > 0 && (
                          <span className="texto-tenue"> · {progreso.porcentaje}% visto</span>
                      )}
                    </span>
                          {progreso?.completado && <span className="episodio__visto">✓ Visto</span>}
                        </button>
                    )
                  })}
                </div>
            ) : (
                <p className="texto-suave">
                  Aún no hay episodios disponibles para este título.
                </p>
            )}
          </div>
        </div>
      </main>
  )
}
