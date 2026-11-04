import { api } from './client'

export const adminApi = {
    // --- Series (ms-catalogo) ---
    buscarEnJikan:          (nombre) => api.get(`/catalogo/admin/jikan/buscar?nombre=${encodeURIComponent(nombre)}`),
    importarPorNombre:      (nombre) => api.post(`/catalogo/admin/animes/importar?nombre=${encodeURIComponent(nombre)}`),
    importarAnimePorNombre: (nombre) => api.post(`/catalogo/admin/animes/importar?nombre=${encodeURIComponent(nombre)}`),
    importarPorMalId:       (malId)  => api.post(`/catalogo/admin/animes/importar?malId=${encodeURIComponent(malId)}`),
    crearAnime:             (datos)  => api.post('/catalogo/admin/animes', datos),
    actualizarAnime:    (id, datos)  => api.put(`/catalogo/admin/animes/${id}`, datos),
    eliminarAnime:      (id)         => api.delete(`/catalogo/admin/animes/${id}`),

    // --- Capitulos y AWS S3 (ms-catalogo) ---
    sugerirEpisodio:  (animeId, numero) =>
        api.get(`/catalogo/admin/episodios/sugerir?animeId=${encodeURIComponent(animeId)}&numero=${encodeURIComponent(numero)}`),
    guardarEpisodio:  (datos) => api.post('/catalogo/admin/episodios', datos),
    crearEpisodio:    (animeId, datos) =>
        api.post('/catalogo/admin/episodios', {
            animeId: Number(animeId),
            numero: Number(datos.numero),
            titulo: datos.titulo,
            duracionSeg: Number(datos.duracionSeg ?? datos.duracionSegundos ?? 1440),
            videoKey: datos.videoKey,
            licencia: datos.licencia || 'CC-BY-3.0',
            fuenteUrl: datos.fuenteUrl || '',
        }),
    eliminarEpisodio: (id)    => api.delete(`/catalogo/admin/episodios/${id}`),
    listarVideosS3:   ()      => api.get('/catalogo/admin/s3/videos'),
    subirVideoS3:     (archivo, carpeta = 'demo') => {
        const formData = new FormData()
        formData.append('archivo', archivo)
        formData.append('carpeta', carpeta)
        return api.upload('/catalogo/admin/s3/upload', formData)
    },

    // --- Sincronizacion Jikan y Traduccion LibreTranslate ---
    sincronizarJikan:   (paginas = 2) => api.post(`/catalogo/admin/sincronizar?paginas=${paginas}`),
    traducirPendientes: ()            => api.post('/catalogo/admin/traducir'),
    traducirSinopsis:   ()            => api.post('/catalogo/admin/traducir'),

    // --- Administracion de Cuentas (ms-auth) ---
    listarUsuarios:    ()          => api.get('/auth/admin/usuarios'),
    actualizarUsuario: (id, datos) => api.put(`/auth/admin/usuarios/${id}`, datos),
    eliminarUsuario:   (id)        => api.delete(`/auth/admin/usuarios/${id}`),
}
