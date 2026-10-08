import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usePerfil } from '../context/PerfilContext'

/**
 * Dos niveles de proteccion:
 *   - autenticacion: hay sesion iniciada
 *   - perfil activo: ademas hay un perfil seleccionado (RF-09)
 *
 * Favoritos y progreso pertenecen a un PERFIL, no a la cuenta. Entrar
 * a esas pantallas sin perfil elegido no tendria sentido.
 */
export default function RutaProtegida({ children, requierePerfil = false }) {
    const { autenticado } = useAuth()
    const { perfilActivo } = usePerfil()
    const ubicacion = useLocation()

    if (!autenticado) {
        // Se guarda el destino para volver aqui tras iniciar sesion
        return <Navigate to="/login" state={{ destino: ubicacion.pathname }} replace />
    }

    if (requierePerfil && !perfilActivo) {
        return <Navigate to="/perfiles" state={{ destino: ubicacion.pathname }} replace />
    }

    return children
}
