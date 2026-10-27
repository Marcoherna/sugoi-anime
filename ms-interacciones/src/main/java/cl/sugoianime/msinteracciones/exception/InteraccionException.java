package cl.sugoianime.msinteracciones.exception;

import org.springframework.http.HttpStatus;

public class InteraccionException extends RuntimeException {

    private final HttpStatus estado;

    public InteraccionException(String mensaje, HttpStatus estado) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    /**
     * Se usa 404 y no 403 a proposito. Responder "prohibido" confirmaria
     * que el perfil existe, permitiendo enumerar perfiles ajenos. Con 404
     * el atacante no distingue entre "no existe" y "no es tuyo".
     */
    public static InteraccionException perfilNoPertenece() {
        return new InteraccionException(
                "El perfil no existe o no pertenece a tu cuenta", HttpStatus.NOT_FOUND);
    }

    public static InteraccionException animeNoExiste(Long animeId) {
        return new InteraccionException(
                "El anime " + animeId + " no existe en el catalogo", HttpStatus.NOT_FOUND);
    }

    public static InteraccionException favoritoDuplicado() {
        return new InteraccionException(
                "El anime ya esta en tus favoritos", HttpStatus.CONFLICT);
    }

    public static InteraccionException favoritoNoEncontrado() {
        return new InteraccionException(
                "El anime no esta en tus favoritos", HttpStatus.NOT_FOUND);
    }

    public static InteraccionException catalogoNoDisponible() {
        return new InteraccionException(
                "El catalogo no esta disponible en este momento",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
