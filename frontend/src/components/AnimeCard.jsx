import { useNavigate } from 'react-router-dom'

export default function AnimeCard({ anime, progreso = null }) {
    const navegar = useNavigate()

    const id = anime.animeId ?? anime.id
    const titulo = anime.titulo ?? 'Sin título'

    return (
        <article className="tarjeta" onClick={() => navegar(`/anime/${id}`)}
                 role="button" tabIndex={0}
                 onKeyDown={(e) => e.key === 'Enter' && navegar(`/anime/${id}`)}>

            {anime.imagenUrl ? (
                <img className="tarjeta__imagen" src={anime.imagenUrl} alt={titulo}
                     loading="lazy" />
            ) : (
                <div className="tarjeta__imagen" style={{ display: 'grid', placeItems: 'center' }}>
                    <span className="texto-tenue" style={{ fontSize: '0.75rem' }}>Sin imagen</span>
                </div>
            )}

            {/* Barra de progreso (RF-03) */}
            {progreso > 0 && (
                <div className="tarjeta__progreso">
                    <div className="tarjeta__progreso-relleno" style={{ width: `${progreso}%` }} />
                </div>
            )}

            <div className="tarjeta__cuerpo">
                <h3 className="tarjeta__titulo">{titulo}</h3>
                <div className="tarjeta__meta">
                    {anime.anio && <span>{anime.anio}</span>}
                    {anime.puntuacion && (
                        <span className="tarjeta__puntaje">★ {Number(anime.puntuacion).toFixed(1)}</span>
                    )}
                </div>
                {anime.puntajeSimilitud != null && (
                    <span className="texto-tenue" style={{ fontSize: '0.72rem' }}>
            Afinidad {Math.round(anime.puntajeSimilitud * 100)}%
          </span>
                )}
            </div>
        </article>
    )
}
