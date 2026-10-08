import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { authApi } from '../api/auth'
import { tokens } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [usuario, setUsuario] = useState(null)
    const [cargando, setCargando] = useState(true)

    // Al montar, si hay token guardado se recupera la sesion.
    // Asi un F5 no obliga a volver a iniciar sesion.
    useEffect(() => {
        let activo = true

        async function recuperarSesion() {
            if (!tokens.access()) {
                setCargando(false)
                return
            }
            try {
                const datos = await authApi.yo()
                if (activo) setUsuario(datos)
            } catch {
                tokens.limpiar()
            } finally {
                if (activo) setCargando(false)
            }
        }

        recuperarSesion()
        return () => { activo = false }
    }, [])

    // El cliente HTTP emite este evento cuando el refresh falla
    useEffect(() => {
        const alExpirar = () => setUsuario(null)
        window.addEventListener('sugoi:sesion-expirada', alExpirar)
        return () => window.removeEventListener('sugoi:sesion-expirada', alExpirar)
    }, [])

    const login = useCallback(async (email, password) => {
        const respuesta = await authApi.login(email, password)
        setUsuario(respuesta.usuario)
        return respuesta
    }, [])

    const registrar = useCallback(async (datos) => {
        const respuesta = await authApi.registrar(datos)
        setUsuario(respuesta.usuario)
        return respuesta
    }, [])

    const logout = useCallback(async () => {
        await authApi.logout()
        setUsuario(null)
        localStorage.removeItem('sugoi_perfil_activo')
    }, [])

    return (
        <AuthContext.Provider
            value={{ usuario, cargando, autenticado: !!usuario, login, registrar, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const contexto = useContext(AuthContext)
    if (!contexto) throw new Error('useAuth debe usarse dentro de AuthProvider')
    return contexto
}
