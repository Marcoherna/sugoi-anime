export default function Cargando({ texto = 'Cargando...' }) {
    return (
        <div className="min-h-[60vh] flex items-center justify-center px-4 pt-20">
            <div className="flex flex-col items-center gap-4 bg-surface-container-low/80 border border-outline-variant/25 rounded-2xl px-8 py-6 shadow-xl backdrop-blur-md">
                <div className="spinner" />
                <span className="text-label-md text-on-surface-variant font-medium">{texto}</span>
            </div>
        </div>
    )
}
