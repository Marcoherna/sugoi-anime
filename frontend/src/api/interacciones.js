import { api } from './client'

export const interaccionesApi = {
    // RF-06
    listarFavoritos: (perfilId, page = 0, size = 20) =>
        api.get(`/interacciones/favoritos?perfilId=${perfilId}&page=${page}&size=${size}`),

    agregarFavorito: (perfilId, animeId) =>
        api.post(`/interacciones/favoritos?perfilId=${perfilId}&animeId=${animeId}`),

    quitarFavorito: (perfilId, animeId) =>
        api.delete(`/interacciones/favoritos/${animeId}?perfilId=${perfilId}`),

    estadoFavorito: (perfilId, animeId) =>
        api.get(`/interacciones/favoritos/estado?perfilId=${perfilId}&animeId=${animeId}`),

    // RF-03
    guardarProgreso: (datos) => api.put('/interacciones/progreso', datos),

    progresoEpisodio: (perfilId, episodioId) =>
        api.get(`/interacciones/progreso/episodio/${episodioId}?perfilId=${perfilId}`),

    progresoAnime: (perfilId, animeId) =>
        api.get(`/interacciones/progreso/anime/${animeId}?perfilId=${perfilId}`),

    continuarViendo: (perfilId, limite = 10) =>
        api.get(`/interacciones/progreso/continuar?perfilId=${perfilId}&limite=${limite}`),

    // RF-07
    recomendaciones: (perfilId) =>
        api.get(`/interacciones/recomendaciones?perfilId=${perfilId}`),

    regenerarRecomendaciones: (perfilId) =>
        api.post(`/interacciones/recomendaciones/regenerar?perfilId=${perfilId}`),
}
