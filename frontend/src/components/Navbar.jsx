import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usePerfil } from '../context/PerfilContext'

export default function Navbar() {
    const { estaAutenticado, esAdmin, cerrarSesion } = useAuth()
    const { perfilActivo, limpiar } = usePerfil()
    const navegar = useNavigate()

    async function salir() {
        limpiar()
        await cerrarSesion()
        navegar('/login')
    }

    const linkClass = ({ isActive }) =>
        isActive
            ? 'px-3 py-1.5 rounded-lg text-label-md font-bold text-primary bg-surface-container-high shadow-[0_0_12px_rgba(208,188,255,0.15)] transition-all flex items-center gap-1.5'
            : 'px-3 py-1.5 rounded-lg text-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low transition-colors flex items-center gap-1.5'

    return (
        <header className="fixed top-0 left-0 right-0 z-50 h-16 bg-surface-container-lowest/80 backdrop-blur-xl border-b border-outline-variant/25 transition-all duration-300">
            <div className="max-w-[1440px] mx-auto h-full px-margin md:px-margin-tablet lg:px-margin-desktop flex items-center justify-between gap-4">
                {/* Brand & Primary Nav */}
                <div className="flex items-center gap-6 lg:gap-8">
                    <Link to="/" className="flex items-center gap-2 group">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-primary-container flex items-center justify-center shadow-[0_0_15px_rgba(160,120,255,0.4)] group-hover:scale-105 transition-transform">
                            <span className="material-symbols-outlined text-on-primary text-xl font-bold">bolt</span>
                        </div>
                        <span className="font-headline-xl text-xl tracking-tight font-extrabold text-on-surface">
                            Sugoi<span className="text-primary">Anime</span>
                        </span>
                    </Link>

                    <nav className="hidden md:flex items-center gap-1">
                        <NavLink to="/" end className={linkClass}>
                            Inicio
                        </NavLink>
                        <NavLink to="/catalogo" className={linkClass}>
                            Catálogo
                        </NavLink>
                        {estaAutenticado && (
                            <NavLink to="/favoritos" className={linkClass}>
                                Favoritos
                            </NavLink>
                        )}
                        {estaAutenticado && esAdmin && (
                            <NavLink to="/admin" className={linkClass}>
                                <span className="material-symbols-outlined text-[16px]">admin_panel_settings</span>
                                Administración
                            </NavLink>
                        )}
                    </nav>
                </div>

                {/* User Actions & Profile */}
                <div className="flex items-center gap-3">
                    {estaAutenticado ? (
                        <>
                            {perfilActivo && (
                                <button
                                    type="button"
                                    onClick={() => navegar('/perfiles')}
                                    title="Cambiar perfil"
                                    className="flex items-center gap-2.5 pl-1.5 pr-3 py-1 rounded-full bg-surface-container-low hover:bg-surface-container-high border border-outline-variant/30 transition-all group"
                                >
                                    <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-primary-container to-secondary-container flex items-center justify-center text-xs font-bold text-on-primary shadow-inner">
                                        {perfilActivo.nombre.charAt(0).toUpperCase()}
                                    </div>
                                    <span className="text-label-md text-on-surface group-hover:text-primary transition-colors">
                                        {perfilActivo.nombre}
                                    </span>
                                    {perfilActivo.esInfantil && (
                                        <span className="px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 text-[10px] font-bold uppercase tracking-wider">
                                            Kids
                                        </span>
                                    )}
                                    <span className="material-symbols-outlined text-sm text-on-surface-variant group-hover:text-on-surface">
                                        expand_more
                                    </span>
                                </button>
                            )}
                            <button
                                type="button"
                                onClick={salir}
                                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-label-md text-on-surface-variant hover:text-error hover:bg-error-container/20 border border-transparent hover:border-error/30 transition-all"
                            >
                                <span className="material-symbols-outlined text-lg">logout</span>
                                <span className="hidden sm:inline">Salir</span>
                            </button>
                        </>
                    ) : (
                        <>
                            <Link
                                to="/login"
                                className="px-3.5 py-1.5 rounded-lg bg-surface-container-low hover:bg-surface-container-high text-on-surface text-label-md font-medium border border-outline-variant/30 transition-all"
                            >
                                Iniciar sesión
                            </Link>
                            <Link
                                to="/registro"
                                className="px-4 py-1.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-on-primary text-label-md font-bold shadow-[0_4px_15px_rgba(160,120,255,0.3)] hover:brightness-110 transition-all"
                            >
                                Registrarse
                            </Link>
                        </>
                    )}
                </div>
            </div>

            {/* Mobile bottom-row subnav */}
            <div className="flex md:hidden items-center justify-around bg-surface-container-lowest/95 backdrop-blur-xl border-b border-outline-variant/20 py-1.5 px-2">
                <NavLink to="/" end className={linkClass}>Inicio</NavLink>
                <NavLink to="/catalogo" className={linkClass}>Catálogo</NavLink>
                {estaAutenticado && <NavLink to="/favoritos" className={linkClass}>Favoritos</NavLink>}
                {estaAutenticado && esAdmin && <NavLink to="/admin" className={linkClass}>Admin</NavLink>}
            </div>
        </header>
    )
}
