import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { catalogoApi } from '../api/catalogo'
import { interaccionesApi } from '../api/interacciones'
import { usePerfil } from '../context/PerfilContext'
import { useAuth } from '../context/AuthContext'
import FilaAnimes from '../components/FilaAnimes'
import Cargando from '../components/Cargando'

export default function Home() {
  const { autenticado } = useAuth()
  const { perfilActivo } = usePerfil()

  const [top, setTop] = useState([])
  const [continuar, setContinuar] = useState([])
  const [recomendados, setRecomendados] = useState([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true

    async function cargar() {
      setCargando(true)

      // Promise.allSettled y no Promise.all: si el servicio de
      // recomendaciones falla, el resto del home debe renderizar
      // igual. Es la degradacion elegante del lado del cliente.
      const promesas = [catalogoApi.top(12)]

      if (perfilActivo) {
        promesas.push(
            interaccionesApi.continuarViendo(perfilActivo.id, 10),
            interaccionesApi.recomendaciones(perfilActivo.id)
        )
      }

      const resultados = await Promise.allSettled(promesas)
      if (!activo) return

      if (resultados[0]?.status === 'fulfilled') {
        setTop(resultados[0].value?.contenido || [])
      }
      if (resultados[1]?.status === 'fulfilled') {
        setContinuar(resultados[1].value || [])
      }
      if (resultados[2]?.status === 'fulfilled') {
        setRecomendados(resultados[2].value || [])
      }

      setCargando(false)
    }

    cargar()
    return () => { activo = false }
  }, [perfilActivo])

  if (cargando) return <Cargando texto="Preparando tu inicio..." />

  return (
      <main className="contenedor pagina">
        {!autenticado && (
            <section className="alerta alerta--info mb-6">
              <strong>Bienvenido a SugoiAnime.</strong>{' '}
              Puedes explorar el catálogo libremente.{' '}
              <Link to="/registro">Crea una cuenta</Link> para reproducir contenido,
              guardar favoritos y recibir recomendaciones personalizadas.
            </section>
        )}

        {/* RF-03: continuar viendo */}
        <FilaAnimes titulo="Continuar viendo" animes={continuar} />

        {/* RF-07: recomendaciones */}
        <FilaAnimes
            titulo="Recomendados para ti"
            subtitulo={recomendados.length > 0 ? 'Según tus gustos' : null}
            animes={recomendados}
            vacio={perfilActivo && recomendados.length === 0
                ? 'Marca al menos dos animes como favoritos para recibir recomendaciones.'
                : null} />

        <FilaAnimes titulo="Mejor valorados" animes={top} />
      </main>
  )
}
