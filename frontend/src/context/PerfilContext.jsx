import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { authApi } from '../api/auth'
import { useAuth } from './AuthContext'

const PerfilContext = createContext(null)
const CLAVE_PERFIL = 'sugoi_perfil_activo'

/**
 * Perfil de visualizacion activo (RF-09).
 *
 * Se separa de AuthContext porque son conceptos distintos: la CUENTA
 * identifica quien paga y quien consiente el tratamiento de datos; el
 * PERFIL identifica de quien es el historial y los favoritos. Una
 * cuenta tiene varios perfiles y cambiar de perfil no cierra sesion.
 */
export function PerfilProvider({ children }) {
    const { autenticado } = useAuth()
    const [perfiles, setPerfiles] = useState([])
    const [perfilActivo, setPerfilActivo] = useState(null)
    const [cargando, setCargando] = useState(false)

    const cargarPerfiles = useCallback(async () => {
        setCargando(true)
        try {
            const lista = await authApi.listarPerfiles()
            setPerfiles(lista)

            const guardadoId = localStorage.getItem(CLAVE_PERFIL)
            const guardado = lista.find((p) => String(p.id) === guardadoId)

            // Con un solo perfil no tiene sentido mostrar el selector
            if (guardado) setPerfilActivo(guardado)
            else if (lista.length === 1) seleccionar(lista[0])

            return lista
        } finally {
            setCargando(false)
        }
    }, [])

    useEffect(() => {
        if (autenticado) {
            cargarPerfiles()
        } else {
            setPerfiles([])
            setPerfilActivo(null)
        }
    }, [autenticado, cargarPerfiles])

    function seleccionar(perfil) {
        setPerfilActivo(perfil)
        localStorage.setItem(CLAVE_PERFIL, String(perfil.id))
    }

    function limpiarSeleccion() {
        setPerfilActivo(null)
        localStorage.removeItem(CLAVE_PERFIL)
    }

    const crear = useCallback(async (datos) => {
        const nuevo = await authApi.crearPerfil(datos)
        setPerfiles((prev) => [...prev, nuevo])
        return nuevo
    }, [])

    const eliminar = useCallback(async (id) => {
        await authApi.eliminarPerfil(id)
        setPerfiles((prev) => prev.filter((p) => p.id !== id))
        if (perfilActivo?.id === id) limpiarSeleccion()
    }, [perfilActivo])

    return (
        <PerfilContext.Provider
            value={{ perfiles, perfilActivo, cargando, seleccionar,
                limpiarSeleccion, cargarPerfiles, crear, eliminar }}>
            {children}
        </PerfilContext.Provider>
    )
}

export function usePerfil() {
    const contexto = useContext(PerfilContext)
    if (!contexto) throw new Error('usePerfil debe usarse dentro de PerfilProvider')
    return contexto
}
