import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import RutaProtegida from './components/RutaProtegida'
import Cargando from './components/Cargando'
import { useAuth } from './context/AuthContext'

import Home from './pages/Home'
import Catalogo from './pages/Catalogo'
import DetalleAnime from './pages/DetalleAnime'
import Reproductor from './pages/Reproductor'
import Login from './pages/Login'
import Registro from './pages/Registro'
import SelectorPerfil from './pages/SelectorPerfil'
import MisFavoritos from './pages/MisFavoritos'

export default function App() {
  const { cargando } = useAuth()

  // Evita el parpadeo de "no autenticado" mientras se recupera la sesion
  if (cargando) return <Cargando texto="Cargando SugoiAnime..." />

  return (
      <>
        <Navbar />
        <Routes>
          {/* Publicas: el catalogo se navega sin cuenta */}
          <Route path="/" element={<Home />} />
          <Route path="/catalogo" element={<Catalogo />} />
          <Route path="/anime/:id" element={<DetalleAnime />} />
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />

          {/* Requieren sesion */}
          <Route path="/perfiles" element={
                                     <RutaProtegida><SelectorPerfil /></RutaProtegida>} />

          <Route path="/favoritos" element={
                                      <RutaProtegida requierePerfil><MisFavoritos /></RutaProtegida>} />

          <Route path="/ver/:episodioId" element={
                                            <RutaProtegida requierePerfil><Reproductor /></RutaProtegida>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </>
  )
}
