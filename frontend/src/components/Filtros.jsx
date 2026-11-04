import { useEffect, useState } from 'react'
import { catalogoApi } from '../api/catalogo'

const ANIO_ACTUAL = new Date().getFullYear()
const ANIOS = Array.from({ length: 35 }, (_, i) => ANIO_ACTUAL - i)

export default function Filtros({ valores, onCambio }) {
    const [generos, setGeneros] = useState([])
    const [texto, setTexto] = useState(valores.texto || '')

    useEffect(() => {
        catalogoApi.generos().then(setGeneros).catch(() => setGeneros([]))
    }, [])

    useEffect(() => {
        setTexto(valores.texto || '')
    }, [valores.texto])

    // Debounce de 400 ms para no saturar la API mientras se escribe
    useEffect(() => {
        const temporizador = setTimeout(() => {
            if (texto !== (valores.texto || '')) onCambio({ ...valores, texto, page: 0 })
        }, 400)
        return () => clearTimeout(temporizador)
    }, [texto])

    return (
        <section
            aria-label="Herramientas de filtrado de catálogo"
            className="mb-8 p-4 md:p-5 rounded-xl bg-surface-container-low border border-outline-variant/25 shadow-lg"
        >
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-4 items-end">
                {/* 1. Buscar Input */}
                <div className="sm:col-span-2 lg:col-span-4 flex flex-col gap-1.5">
                    <label
                        className="text-label-sm text-on-surface-variant uppercase tracking-wider"
                        htmlFor="f-texto"
                    >
                        Buscar
                    </label>
                    <div className="relative">
                        <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-outline text-lg pointer-events-none">
                            search
                        </span>
                        <input
                            id="f-texto"
                            type="search"
                            placeholder="Título del anime..."
                            value={texto}
                            onChange={(e) => setTexto(e.target.value)}
                            className="w-full pl-10 pr-4 py-2.5 bg-surface-container-lowest text-on-surface placeholder-outline text-label-md rounded-lg border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition-all"
                        />
                    </div>
                </div>

                {/* 2. Género Select */}
                <div className="lg:col-span-3 flex flex-col gap-1.5">
                    <label
                        className="text-label-sm text-on-surface-variant uppercase tracking-wider"
                        htmlFor="f-genero"
                    >
                        Género
                    </label>
                    <div className="relative">
                        <select
                            id="f-genero"
                            value={valores.genero || ''}
                            onChange={(e) => onCambio({ ...valores, genero: e.target.value, page: 0 })}
                            className="w-full pl-3.5 pr-9 py-2.5 bg-surface-container-lowest text-on-surface text-label-md rounded-lg border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none appearance-none transition-all cursor-pointer"
                        >
                            <option value="">Todos los géneros</option>
                            {generos.map((g) => (
                                <option key={g.id} value={g.nombre}>
                                    {g.nombre}
                                </option>
                            ))}
                        </select>
                        <span className="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-outline text-lg pointer-events-none">
                            unfold_more
                        </span>
                    </div>
                </div>

                {/* 3. Año Select */}
                <div className="lg:col-span-2 flex flex-col gap-1.5">
                    <label
                        className="text-label-sm text-on-surface-variant uppercase tracking-wider"
                        htmlFor="f-anio"
                    >
                        Año
                    </label>
                    <div className="relative">
                        <select
                            id="f-anio"
                            value={valores.anio || ''}
                            onChange={(e) => onCambio({ ...valores, anio: e.target.value, page: 0 })}
                            className="w-full pl-3.5 pr-8 py-2.5 bg-surface-container-lowest text-on-surface text-label-md rounded-lg border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none appearance-none transition-all cursor-pointer"
                        >
                            <option value="">Todos</option>
                            {ANIOS.map((a) => (
                                <option key={a} value={a}>
                                    {a}
                                </option>
                            ))}
                        </select>
                        <span className="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-outline text-lg pointer-events-none">
                            calendar_today
                        </span>
                    </div>
                </div>

                {/* 4. Ordenar por Select */}
                <div className="lg:col-span-2 flex flex-col gap-1.5">
                    <label
                        className="text-label-sm text-on-surface-variant uppercase tracking-wider"
                        htmlFor="f-orden"
                    >
                        Ordenar por
                    </label>
                    <div className="relative">
                        <select
                            id="f-orden"
                            value={valores.sort}
                            onChange={(e) => onCambio({ ...valores, sort: e.target.value, page: 0 })}
                            className="w-full pl-3.5 pr-8 py-2.5 bg-surface-container-lowest text-on-surface text-label-md rounded-lg border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none appearance-none transition-all cursor-pointer"
                        >
                            <option value="puntuacion,desc">Mejor valorados</option>
                            <option value="anio,desc">Más recientes</option>
                            <option value="titulo,asc">Título (A - Z)</option>
                        </select>
                        <span className="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-outline text-lg pointer-events-none">
                            sort
                        </span>
                    </div>
                </div>

                {/* 5. Limpiar Button */}
                <div className="sm:col-span-2 lg:col-span-1 flex">
                    <button
                        type="button"
                        onClick={() => {
                            setTexto('')
                            onCambio({ page: 0, size: 20, sort: 'puntuacion,desc' })
                        }}
                        title="Restablecer filtros"
                        className="w-full py-2.5 px-3 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all flex items-center justify-center gap-1.5 active:scale-95"
                    >
                        <span className="material-symbols-outlined text-lg">filter_alt_off</span>
                        <span className="lg:hidden xl:inline">Limpiar</span>
                    </button>
                </div>
            </div>
        </section>
    )
}
