import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { interaccionesApi } from '../api/interacciones'
import { usePerfil } from '../context/PerfilContext'
import AnimeCard from '../components/AnimeCard'
import Cargando from '../components/Cargando'

export default function MisFavoritos() {
  const { perfilActivo } = usePerfil()
  const [pagina, setPagina] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!perfilActivo) return
    let activo = true
    setCargando(true)

    interaccionesApi.listarFavoritos(perfilActivo.id)
        .then((datos) => { if (activo) setPagina(datos) })
        .catch((ex) => { if (activo) setError(ex.message) })
        .finally(() => { if (activo) setCargando(false) })

    return () => { activo = false }
  }, [perfilActivo])

  if (cargando) return <Cargando />

  const favoritos = pagina?.content || []
  const incompletos = favoritos.filter((f) => f.datosCompletos === false).length

  return (
      <main className="contenedor pagina">
        <h1>Favoritos de {perfilActivo?.nombre}</h1>

        {error && <div className="alerta alerta--error">{error}</div>}

        {/* Degradacion visible: si ms-catalogo esta caido, se avisa sin
          ocultar la informacion que si tenemos. */}
        {incompletos > 0 && (
            <div className="alerta alerta--info">
              No se pudieron cargar los detalles de {incompletos} título
              {incompletos !== 1 ? 's' : ''}. El servicio de catálogo podría estar
              temporalmente no disponible.
            </div>
        )}

        {favoritos.length > 0 ? (
            <div className="grilla-animes">
              {favoritos.map((favorito) => (
                  <AnimeCard key={favorito.favoritoId} anime={favorito} />
              ))}
            </div>
        ) : (
            <div className="vacio">
              <p>Aún no tienes favoritos en este perfil.</p>
              <Link to="/catalogo" className="boton">Explorar el catálogo</Link>
            </div>
        )}
      </main>
  )
}
