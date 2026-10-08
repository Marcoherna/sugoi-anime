import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
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
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [segundoInicial, setSegundoInicial] = useState(0)

  const { alActualizarTiempo, alPausar, alTerminar } = useProgreso({
    perfilId: perfilActivo?.id,
    animeId: datos?.animeId,
    episodioId: Number(episodioId),
    numeroEpisodio: datos?.numeroEpisodio,
    duracionSeg: datos?.duracionSeg,
  })

  useEffect(() => {
    let activo = true
    setCargando(true)

    async function cargar() {
      try {
        const reproduccion = await catalogoApi.reproduccion(episodioId)
        if (!activo) return
        setDatos(reproduccion)

        // RF-03: recupera el punto donde quedo
        if (perfilActivo) {
          const progreso = await interaccionesApi
              .progresoEpisodio(perfilActivo.id, episodioId)
          if (activo && progreso?.segundo > 0 && !progreso.completado) {
            setSegundoInicial(progreso.segundo)
          }
        }
      } catch (ex) {
        if (activo) setError(ex.message)
      } finally {
        if (activo) setCargando(false)
      }
    }

    cargar()
    return () => { activo = false }
  }, [episodioId, perfilActivo])

  // Reanuda en el segundo guardado cuando el video tiene metadatos
  function alCargarMetadatos() {
    if (segundoInicial > 0 && videoRef.current) {
      videoRef.current.currentTime = segundoInicial
    }
  }

  if (cargando) return <Cargando texto="Preparando el reproductor..." />
  if (error) return <main className="contenedor pagina">
    <div className="alerta alerta--error">{error}</div></main>
  if (!datos) return null

  return (
      <main className="contenedor pagina">
        <div className="reproductor">
          <video
              ref={videoRef}
              src={datos.urlVideo}
              controls
              autoPlay
              playsInline
              onLoadedMetadata={alCargarMetadatos}
              onTimeUpdate={alActualizarTiempo}
              onPause={alPausar}
              onEnded={alTerminar}
          />

          <div className="reproductor__info">
            <h2 style={{ marginBottom: '0.25rem' }}>{datos.tituloAnime}</h2>
            <p className="texto-suave" style={{ marginBottom: '0.5rem' }}>
              Episodio {datos.numeroEpisodio}
            </p>

            {segundoInicial > 0 && (
                <div className="alerta alerta--info">
                  Reanudando desde el minuto {Math.floor(segundoInicial / 60)}:
                  {String(Math.floor(segundoInicial % 60)).padStart(2, '0')}
                </div>
            )}

            {/* Trazabilidad Ley 17.336 - riesgo R04 */}
            {datos.licencia && (
                <span className="licencia">
              Licencia del contenido: {datos.licencia}
            </span>
            )}

            <div className="flex-fila mt-4">
              <button className="boton boton--secundario"
                      onClick={() => navegar(`/anime/${datos.animeId}`)}>
                Volver a la ficha
              </button>
            </div>
          </div>
        </div>
      </main>
  )
}
