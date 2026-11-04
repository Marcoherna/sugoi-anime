import { Routes, Route } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import RutaProtegida from './components/RutaProtegida'
import Cargando from './components/Cargando'

import Home from './pages/Home'
import Login from './pages/Login'
import Registro from './pages/Registro'
import SelectorPerfil from './pages/SelectorPerfil'
import Catalogo from './pages/Catalogo'
import DetalleAnime from './pages/DetalleAnime'
import Reproductor from './pages/Reproductor'
import MisFavoritos from './pages/MisFavoritos'
import Admin from './pages/Admin'
import NoEncontrado from './pages/NoEncontrado'

export default function App() {
    const { cargando } = useAuth()

    if (cargando) return <Cargando texto="Cargando SugoiAnime..." />

    return (
        <>
            <Navbar />
            <div className="flex-1 flex flex-col">
                <Routes>
                    {/* Rutas públicas */}
                    <Route path="/" element={<Home />} />
                    <Route path="/catalogo" element={<Catalogo />} />
                    <Route path="/anime/:id" element={<DetalleAnime />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/registro" element={<Registro />} />

                    {/* Requiere cuenta autenticada */}
                    <Route
                        path="/perfiles"
                        element={
                            <RutaProtegida>
                                <SelectorPerfil />
                            </RutaProtegida>
                        }
                    />

                    {/* Panel de administración (requiere cuenta con rol ADMIN) */}
                    <Route
                        path="/admin"
                        element={
                            <RutaProtegida requiereAdmin>
                                <Admin />
                            </RutaProtegida>
                        }
                    />

                    {/* Requieren cuenta + perfil activo */}
                    <Route
                        path="/ver/:episodioId"
                        element={
                            <RutaProtegida requierePerfil>
                                <Reproductor />
                            </RutaProtegida>
                        }
                    />
                    <Route
                        path="/favoritos"
                        element={
                            <RutaProtegida requierePerfil>
                                <MisFavoritos />
                            </RutaProtegida>
                        }
                    />

                    <Route path="*" element={<NoEncontrado />} />
                </Routes>
            </div>

            {/* Global Obsidian Neon Cinematic Footer */}
            <footer className="mt-16 border-t border-outline-variant/20 bg-surface-container-low/50 py-8">
                <div className="max-w-[1440px] mx-auto px-margin md:px-margin-tablet lg:px-margin-desktop flex flex-col sm:flex-row items-center justify-between gap-4 text-caption text-on-surface-variant">
                    <div className="flex items-center gap-2.5">
                        <img
                            src="/logo.png"
                            alt="SugoiAnime"
                            className="w-7 h-7 rounded-lg object-contain shadow-[0_0_10px_rgba(160,120,255,0.35)]"
                        />
                        <span className="font-semibold text-on-surface">SugoiAnime</span>
                        <span>— Plataforma de Streaming en Español</span>
                    </div>
                    <div className="flex items-center gap-6">
                        <span className="text-xs text-outline">© {new Date().getFullYear()} SugoiAnime</span>
                    </div>
                </div>
            </footer>
        </>
    )
}
