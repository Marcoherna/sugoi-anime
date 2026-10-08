import AnimeCard from './AnimeCard'

export default function FilaAnimes({ titulo, subtitulo, animes, vacio }) {
    if (!animes || animes.length === 0) {
        return vacio ? (
            <section className="fila">
                <div className="fila__cabecera"><h2>{titulo}</h2></div>
                <p className="texto-suave">{vacio}</p>
            </section>
        ) : null
    }

    return (
        <section className="fila">
            <div className="fila__cabecera">
                <h2>{titulo}</h2>
                {subtitulo && <span className="texto-tenue">{subtitulo}</span>}
            </div>
            <div className="fila__scroll">
                {animes.map((a) => (
                    <AnimeCard key={a.animeId ?? a.id} anime={a} progreso={a.porcentaje} />
                ))}
            </div>
        </section>
    )
}
