import { Link } from 'react-router-dom'

export default function NoEncontrado() {
    return (
        <main className="flex-1 flex items-center justify-center pt-24 pb-16 px-4 text-center">
            <div className="max-w-md space-y-4 bg-surface-container-low border border-outline-variant/25 rounded-2xl p-8 shadow-2xl">
                <span className="font-display-hero text-5xl font-extrabold text-primary block">404</span>
                <h1 className="font-title-card text-xl font-bold text-on-surface">Página no encontrada</h1>
                <p className="text-caption text-on-surface-variant">
                    La ruta que buscas no existe o fue movida dentro de SugoiAnime.
                </p>
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-primary to-primary-container text-on-primary font-bold text-label-md"
                >
                    <span className="material-symbols-outlined text-lg">home</span>
                    <span>Volver al inicio</span>
                </Link>
            </div>
        </main>
    )
}

