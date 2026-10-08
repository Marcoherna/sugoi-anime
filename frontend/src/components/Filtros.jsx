import { useEffect, useState } from 'react'
import { catalogoApi } from '../api/catalogo'

const ANIO_ACTUAL = new Date().getFullYear()
const ANIOS = Array.from({ length: 30 }, (_, i) => ANIO_ACTUAL - i)

export default function Filtros({ valores, onCambio }) {
    const [generos, setGeneros] = useState([])
    const [texto, setTexto] = useState(valores.texto || '')

    useEffect(() => {
        catalogoApi.generos().then(setGeneros).catch(() => setGeneros([]))
    }, [])

    // Debounce: sin esto, cada tecla dispara una peticion al backend.
    // 400 ms es el punto donde deja de sentirse lento sin saturar la API.
    useEffect(() => {
        const temporizador = setTimeout(() => {
            if (texto !== (valores.texto || '')) onCambio({ ...valores, texto, page: 0 })
        }, 400)
        return () => clearTimeout(temporizador)
    }, [texto])

    return (
        <div className="filtros">
            <div className="filtros__campo">
                <label className="campo__etiqueta" htmlFor="f-texto">Buscar</label>
                <input id="f-texto" className="campo__input" type="search"
                       placeholder="Título del anime..."
                       value={texto} onChange={(e) => setTexto(e.target.value)} />
            </div>

            <div className="filtros__campo">
                <label className="campo__etiqueta" htmlFor="f-genero">Género</label>
                <select id="f-genero" className="campo__select" value={valores.genero || ''}
                        onChange={(e) => onCambio({ ...valores, genero: e.target.value, page: 0 })}>
                    <option value="">Todos</option>
                    {generos.map((g) => (
                        <option key={g.id} value={g.nombre}>{g.nombre}</option>
                    ))}
                </select>
            </div>

            <div className="filtros__campo">
                <label className="campo__etiqueta" htmlFor="f-anio">Año</label>
                <select id="f-anio" className="campo__select" value={valores.anio || ''}
                        onChange={(e) => onCambio({ ...valores, anio: e.target.value, page: 0 })}>
                    <option value="">Todos</option>
                    {ANIOS.map((a) => <option key={a} value={a}>{a}</option>)}
                </select>
            </div>

            <div className="filtros__campo">
                <label className="campo__etiqueta" htmlFor="f-orden">Ordenar por</label>
                <select id="f-orden" className="campo__select" value={valores.sort}
                        onChange={(e) => onCambio({ ...valores, sort: e.target.value, page: 0 })}>
                    <option value="puntuacion,desc">Mejor valorados</option>
                    <option value="anio,desc">Más recientes</option>
                    <option value="titulo,asc">Título A-Z</option>
                </select>
            </div>

            <button className="boton boton--secundario"
                    onClick={() => { setTexto(''); onCambio({ page: 0, size: 20, sort: 'puntuacion,desc' }) }}>
                Limpiar
            </button>
        </div>
    )
}
