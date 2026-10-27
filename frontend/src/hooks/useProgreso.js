import { useCallback, useEffect, useRef } from 'react'
import { interaccionesApi } from '../api/interacciones'

const INTERVALO_MS = 10_000

/**
 * Guardado de progreso con throttling (RF-03).
 *
 * PROBLEMA: el evento timeupdate del elemento <video> se dispara entre
 * 4 y 66 veces por segundo. Llamar al backend en cada uno seria una
 * denegacion de servicio autoinfligida.
 *
 * SOLUCION: se guarda como maximo cada 10 segundos, mas un guardado
 * inmediato al pausar, al terminar y al cerrar la pestana. Asi se pasa
 * de miles de peticiones por episodio a unas 150, sin que el usuario
 * pierda mas de 10 segundos de avance en el peor caso.
 */
export function useProgreso({ perfilId, animeId, episodioId, numeroEpisodio, duracionSeg }) {
    const ultimoGuardado = useRef(0)
    const segundoActual = useRef(0)

    const guardar = useCallback(async (segundo, forzar = false) => {
        if (!perfilId || !episodioId) return

        const ahora = Date.now()
        if (!forzar && ahora - ultimoGuardado.current < INTERVALO_MS) return

        ultimoGuardado.current = ahora

        try {
            await interaccionesApi.guardarProgreso({
                perfilId, animeId, episodioId, numeroEpisodio,
                segundo: Math.floor(segundo),
                duracionSeg: duracionSeg ? Math.floor(duracionSeg) : null,
            })
        } catch {
            // Fallo silencioso a proposito: perder un punto de progreso no
            // debe interrumpir la reproduccion con un mensaje de error.
        }
    }, [perfilId, animeId, episodioId, numeroEpisodio, duracionSeg])

    const alActualizarTiempo = useCallback((evento) => {
        segundoActual.current = evento.target.currentTime
        guardar(evento.target.currentTime)
    }, [guardar])

    const alPausar = useCallback((evento) => {
        guardar(evento.target.currentTime, true)
    }, [guardar])

    const alTerminar = useCallback((evento) => {
        guardar(evento.target.duration, true)
    }, [guardar])

    // Guardado al cerrar la pestana o cambiar de app.
    // visibilitychange es mas fiable que beforeunload en moviles, donde
    // el navegador puede matar la pagina sin dispararlo.
    useEffect(() => {
        function alOcultar() {
            if (document.visibilityState === 'hidden' && segundoActual.current > 0) {
                guardar(segundoActual.current, true)
            }
        }
        document.addEventListener('visibilitychange', alOcultar)
        return () => {
            document.removeEventListener('visibilitychange', alOcultar)
            if (segundoActual.current > 0) guardar(segundoActual.current, true)
        }
    }, [guardar])

    return { alActualizarTiempo, alPausar, alTerminar }
}
