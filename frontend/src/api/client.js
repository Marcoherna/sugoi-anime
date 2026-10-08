const BASE = import.meta.env.VITE_API_URL || '/api'

const CLAVE_ACCESS = 'sugoi_access_token'
const CLAVE_REFRESH = 'sugoi_refresh_token'

export const tokens = {
    access:  () => localStorage.getItem(CLAVE_ACCESS),
    refresh: () => localStorage.getItem(CLAVE_REFRESH),

    guardar(accessToken, refreshToken) {
        localStorage.setItem(CLAVE_ACCESS, accessToken)
        if (refreshToken) localStorage.setItem(CLAVE_REFRESH, refreshToken)
    },

    limpiar() {
        localStorage.removeItem(CLAVE_ACCESS)
        localStorage.removeItem(CLAVE_REFRESH)
    },
}

export class ApiError extends Error {
    constructor(mensaje, estado, cuerpo) {
        super(mensaje)
        this.estado = estado
        this.cuerpo = cuerpo
    }
}

/**
 * Renovacion de token con guardia de concurrencia.
 *
 * Si el home dispara cuatro peticiones a la vez y todas reciben 401,
 * sin esta guardia se lanzarian cuatro refresh simultaneos. Como el
 * backend ROTA el refresh token (revoca el usado y emite uno nuevo),
 * el primero invalidaria a los otros tres y el usuario quedaria
 * deslogueado sin motivo. La promesa compartida hace que solo se
 * ejecute un refresh y los demas esperen su resultado.
 */
let refrescoEnCurso = null

async function refrescarToken() {
    if (refrescoEnCurso) return refrescoEnCurso

    refrescoEnCurso = (async () => {
        const refreshToken = tokens.refresh()
        if (!refreshToken) throw new ApiError('Sin refresh token', 401)

        const respuesta = await fetch(`${BASE}/auth/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        })

        if (!respuesta.ok) {
            tokens.limpiar()
            throw new ApiError('Sesion expirada', 401)
        }

        const datos = await respuesta.json()
        tokens.guardar(datos.accessToken, datos.refreshToken)
        return datos.accessToken
    })()

    try {
        return await refrescoEnCurso
    } finally {
        refrescoEnCurso = null
    }
}

async function ejecutar(ruta, opciones = {}, reintentar = true) {
    const cabeceras = { ...(opciones.headers || {}) }

    if (opciones.body && !cabeceras['Content-Type']) {
        cabeceras['Content-Type'] = 'application/json'
    }

    const accessToken = tokens.access()
    if (accessToken && !opciones.sinAuth) {
        cabeceras['Authorization'] = `Bearer ${accessToken}`
    }

    const respuesta = await fetch(`${BASE}${ruta}`, { ...opciones, headers: cabeceras })

    // 401 con token presente: intenta renovar una sola vez
    if (respuesta.status === 401 && reintentar && accessToken && !opciones.sinAuth) {
        try {
            await refrescarToken()
            return ejecutar(ruta, opciones, false)
        } catch {
            tokens.limpiar()
            window.dispatchEvent(new CustomEvent('sugoi:sesion-expirada'))
            throw new ApiError('Sesion expirada', 401)
        }
    }

    if (respuesta.status === 204) return null

    const texto = await respuesta.text()
    const cuerpo = texto ? JSON.parse(texto) : null

    if (!respuesta.ok) {
        throw new ApiError(
            cuerpo?.mensaje || `Error ${respuesta.status}`,
            respuesta.status,
            cuerpo
        )
    }

    return cuerpo
}

export const api = {
    get:    (ruta, opciones)        => ejecutar(ruta, { ...opciones, method: 'GET' }),
    post:   (ruta, datos, opciones) => ejecutar(ruta, { ...opciones, method: 'POST',
        body: datos ? JSON.stringify(datos) : undefined }),
    put:    (ruta, datos, opciones) => ejecutar(ruta, { ...opciones, method: 'PUT',
        body: datos ? JSON.stringify(datos) : undefined }),
    delete: (ruta, opciones)        => ejecutar(ruta, { ...opciones, method: 'DELETE' }),
}
