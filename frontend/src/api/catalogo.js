import { api } from './client'

function query(parametros) {
    const usp = new URLSearchParams()
    Object.entries(parametros).forEach(([clave, valor]) => {
        if (valor !== null && valor !== undefined && valor !== '') {
            usp.append(clave, valor)
        }
    })
    const s = usp.toString()
    return s ? `?${s}` : ''
}

export const catalogoApi = {
    // RF-01
    listar: ({ genero, anio, texto, page = 0, size = 20, sort = 'puntuacion,desc' } = {}) =>
        api.get(`/catalogo/animes${query({ genero, anio, texto, page, size, sort })}`),

    detalle: (id) => api.get(`/catalogo/animes/${id}`),

    generos: () => api.get('/catalogo/animes/generos'),

    top: (size = 10) => api.get(`/catalogo/animes/top${query({ size })}`),

    // RF-02
    episodios:   (animeId) => api.get(`/catalogo/episodios${query({ animeId })}`),
    reproduccion: (episodioId) => api.get(`/catalogo/episodios/${episodioId}/reproduccion`),
}
