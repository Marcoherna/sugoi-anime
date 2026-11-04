import AnimeCard from './AnimeCard'

export default function FilaAnimes({ titulo, subtitulo, icono = 'auto_awesome', colorIcono = 'text-primary', animes, vacio }) {
    if (!animes || animes.length === 0) {
        return vacio ? (
            <section className="mb-10">
                <div className="flex items-center gap-2.5 mb-4">
                    <span className={`material-symbols-outlined ${colorIcono} text-2xl`}>{icono}</span>
                    <div>
                        <h2 className="font-headline-xl text-xl md:text-2xl font-bold text-on-surface">{titulo}</h2>
                        {subtitulo && <p className="text-caption text-on-surface-variant">{subtitulo}</p>}
                    </div>
                </div>
                <div className="bg-surface-container-low/60 border border-outline-variant/20 rounded-xl p-6 text-on-surface-variant text-sm">
                    {vacio}
                </div>
            </section>
        ) : null
    }

    return (
        <section className="mb-12">
            <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-2 mb-5">
                <div className="flex items-center gap-2.5">
                    <span className={`material-symbols-outlined ${colorIcono} text-2xl`}>{icono}</span>
                    <div>
                        <h2 className="font-headline-xl text-xl md:text-2xl font-bold text-on-surface tracking-tight">
                            {titulo}
                        </h2>
                        {subtitulo && (
                            <p className="text-caption text-on-surface-variant mt-0.5">{subtitulo}</p>
                        )}
                    </div>
                </div>
                <span className="text-xs font-medium text-outline">
                    {animes.length} {animes.length === 1 ? 'título' : 'títulos'}
                </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-gutter lg:gap-gutter-desktop">
                {animes.map((a) => (
                    <AnimeCard
                        key={a.animeId ?? a.id}
                        anime={a}
                        progreso={a.porcentaje}
                        subtituloExtra={a.episodioNumero ? `Episodio ${a.episodioNumero}` : null}
                    />
                ))}
            </div>
        </section>
    )
}
