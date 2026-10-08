import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usePerfil } from '../context/PerfilContext'

export default function Navbar() {
    const { autenticado, logout } = useAuth()
    const { perfilActivo, limpiarSeleccion } = usePerfil()
    const navegar = useNavigate()

    async function cerrarSesion() {
        await logout()
        navegar('/')
    }

    function cambiarPerfil() {
        limpiarSeleccion()
        navegar('/perfiles')
    }

    const claseLink = ({ isActive }) =>
        `navbar__link${isActive ? ' navbar__link--activo' : ''}`

    return (
        <nav className="navbar">
            <div className="contenedor navbar__interior">
                <Link to="/" className="navbar__logo">
                    Sugoi<span>Anime</span>
                </Link>

                <div className="navbar__links">
                    <NavLink to="/" className={claseLink} end>Inicio</NavLink>
                    <NavLink to="/catalogo" className={claseLink}>Catálogo</NavLink>
                    {autenticado && <NavLink to="/favoritos" className={claseLink}>Favoritos</NavLink>}
                </div>

                {autenticado ? (
                    <div className="flex-fila">
                        {perfilActivo && (
                            <button className="navbar__perfil" onClick={cambiarPerfil}
                                    title="Cambiar de perfil">
                <span className="perfil__avatar"
                      style={{ width: 28, height: 28, fontSize: '0.85rem' }}>
                  {perfilActivo.nombre.charAt(0).toUpperCase()}
                </span>
                                {perfilActivo.nombre}
                            </button>
                        )}
                        <button className="boton boton--secundario boton--pequeno"
                                onClick={cerrarSesion}>
                            Salir
                        </button>
                    </div>
                ) : (
                    <div className="flex-fila">
                        <Link to="/login" className="boton boton--secundario boton--pequeno">
                            Iniciar sesión
                        </Link>
                        <Link to="/registro" className="boton boton--pequeno">
                            Registrarse
                        </Link>
                    </div>
                )}
            </div>
        </nav>
    )
}
