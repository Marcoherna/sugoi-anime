export default function Cargando({ texto = 'Cargando...' }) {
    return (
        <div className="cargando">
            <div style={{ display: 'grid', placeItems: 'center', gap: '1rem' }}>
                <div className="spinner" />
                <span className="texto-suave">{texto}</span>
            </div>
        </div>
    )
}
