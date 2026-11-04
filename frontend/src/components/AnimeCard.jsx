import { useNavigate } from 'react-router-dom'

export default function AnimeCard({ anime, progreso = null, etiqueta = null, subtituloExtra = null, accionSuperiorDerecha = null }) {
    const navegar = useNavigate()

    const id = anime.animeId ?? anime.id
    const titulo = anime.titulo ?? 'Sin título'
    const porcentaje = progreso ?? anime.porcentaje ?? null

    return (
        <article
            onClick={() => navegar(`/anime/${id}`)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && navegar(`/anime/${id}`)}
            className="group relative flex flex-col bg-surface-container-low rounded-xl overflow-hidden shadow-md hover:shadow-2xl hover:shadow-primary/10 hover:-translate-y-1.5 transition-all duration-300 cursor-pointer border border-outline-variant/15 hover:border-primary/40"
        >
            <div className="relative aspect-[2/3] w-full overflow-hidden bg-surface-container-highest">
                {anime.imagenUrl ? (
                    <img
                        src={anime.imagenUrl}
                        alt={titulo}
                        loading="lazy"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
                    />
                ) : (
                    <div className="w-full h-full flex flex-col items-center justify-center gap-2 text-outline">
                        <span className="material-symbols-outlined text-3xl">movie</span>
                        <span className="text-xs">Sin portada</span>
                    </div>
                )}

                <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest via-surface-container-lowest/20 to-transparent opacity-85 group-hover:opacity-65 transition-opacity" />

                {/* Top Left Badge */}
                {(etiqueta || anime.puntajeSimilitud != null || (porcentaje != null && porcentaje > 0)) && (
                    <div className="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5 z-10">
                        {etiqueta && (
                            <span className="px-2 py-0.5 rounded bg-surface-container-lowest/80 backdrop-blur-md text-[11px] font-semibold text-primary border border-primary/25">
                                {etiqueta}
                            </span>
                        )}
                        {anime.puntajeSimilitud != null && (
                            <span className="px-2 py-0.5 rounded bg-primary-container/90 backdrop-blur-md text-[11px] font-bold text-on-primary shadow-sm">
                                {Math.round(anime.puntajeSimilitud * 100)}% Match
                            </span>
                        )}
                        {porcentaje != null && porcentaje > 0 && !etiqueta && (
                            <span className="px-2 py-0.5 rounded bg-surface-container-lowest/85 backdrop-blur-md text-[11px] font-semibold text-primary border border-primary/30">
                                {Math.round(porcentaje)}% visto
                            </span>
                        )}
                    </div>
                )}

                {/* Optional Top Right Action (e.g., Remove Favorite button) */}
                {accionSuperiorDerecha && (
                    <div
                        className="absolute top-2.5 right-2.5 z-20"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {accionSuperiorDerecha}
                    </div>
                )}

                {/* Hover Play Indicator */}
                <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-surface-container-lowest/40 backdrop-blur-[2px]">
                    <div className="w-12 h-12 rounded-full bg-primary text-on-primary flex items-center justify-center shadow-[0_0_20px_rgba(208,188,255,0.6)] transform scale-90 group-hover:scale-100 transition-transform">
                        <span
                            className="material-symbols-outlined text-2xl ml-0.5"
                            style={{ fontVariationSettings: "'FILL' 1" }}
                        >
                            play_arrow
                        </span>
                    </div>
                </div>

                {/* Progress Bar (RF-03) */}
                {porcentaje != null && porcentaje > 0 && (
                    <div className="absolute bottom-0 inset-x-0 h-1.5 bg-surface-container-highest/90">
                        <div
                            className="h-full bg-gradient-to-r from-primary to-primary-container shadow-[0_0_8px_#a078ff]"
                            style={{ width: `${Math.min(100, Math.max(0, porcentaje))}%` }}
                        />
                    </div>
                )}
            </div>

            <div className="p-3.5 flex flex-col flex-1 justify-between bg-surface-container-low group-hover:bg-surface-container transition-colors">
                <div>
                    <h3
                        className="font-title-card text-sm sm:text-base font-semibold text-on-surface line-clamp-2 group-hover:text-primary transition-colors leading-snug"
                        title={titulo}
                    >
                        {titulo}
                    </h3>
                    {subtituloExtra && (
                        <p className="text-[11px] text-on-surface-variant mt-1 line-clamp-1">
                            {subtituloExtra}
                        </p>
                    )}
                </div>

                <div className="mt-2.5 pt-2 border-t border-outline-variant/20 flex items-center justify-between text-caption">
                    <span className="text-on-surface-variant font-medium">
                        {anime.anio || anime.estado || 'Serie TV'}
                    </span>
                    {anime.puntuacion != null && Number(anime.puntuacion) > 0 ? (
                        <div className="flex items-center gap-1 px-1.5 py-0.5 rounded bg-tertiary/15 text-tertiary font-semibold text-xs">
                            <span
                                className="material-symbols-outlined text-[14px]"
                                style={{ fontVariationSettings: "'FILL' 1" }}
                            >
                                star
                            </span>
                            <span>{Number(anime.puntuacion).toFixed(1)}</span>
                        </div>
                    ) : (
                        <span className="text-[11px] text-outline">HD</span>
                    )}
                </div>
            </div>
        </article>
    )
}
