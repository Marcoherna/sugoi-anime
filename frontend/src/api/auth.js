import { api, tokens } from './client'

export const authApi = {
    async registrar(datos) {
        const respuesta = await api.post('/auth/auth/registro', datos, { sinAuth: true })
        tokens.guardar(respuesta.accessToken, respuesta.refreshToken)
        return respuesta
    },

    async login(email, password) {
        const respuesta = await api.post('/auth/auth/login', { email, password }, { sinAuth: true })
        tokens.guardar(respuesta.accessToken, respuesta.refreshToken)
        return respuesta
    },

    async logout() {
        const refreshToken = tokens.refresh()
        try {
            if (refreshToken) {
                await api.post('/auth/auth/logout', { refreshToken }, { sinAuth: true })
            }
        } finally {
            // Se limpia siempre, aunque el backend no responda: la sesion
            // local debe terminar pase lo que pase.
            tokens.limpiar()
        }
    },

    yo: () => api.get('/auth/auth/yo'),

    eliminarCuenta: () => api.delete('/auth/auth/cuenta'),

    // --- Perfiles (RF-09) ---
    listarPerfiles: () => api.get('/perfiles'),
    crearPerfil:    (datos) => api.post('/perfiles', datos),
    editarPerfil:   (id, datos) => api.put(`/perfiles/${id}`, datos),
    eliminarPerfil: (id) => api.delete(`/perfiles/${id}`),
}
