import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { PerfilProvider } from './context/PerfilContext'
import App from './App'
import './index.css'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <AuthProvider>
                <PerfilProvider>
                    <App />
                </PerfilProvider>
            </AuthProvider>
        </BrowserRouter>
    </StrictMode>
)
