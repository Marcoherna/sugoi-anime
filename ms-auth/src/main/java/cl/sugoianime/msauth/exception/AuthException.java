package cl.sugoianime.msauth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus estado;

    public AuthException(String mensaje, HttpStatus estado) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    // ---- Fabricas con los casos concretos del dominio ----

    public static AuthException emailYaRegistrado() {
        return new AuthException(
                "Ya existe una cuenta registrada con ese email", HttpStatus.CONFLICT);
    }

    /**
     * Mensaje deliberadamente ambiguo: no revela si el email existe o si
     * la contrasena era incorrecta. Distinguirlos permitiria enumerar
     * usuarios registrados (user enumeration).
     */
    public static AuthException credencialesInvalidas() {
        return new AuthException(
                "Email o contrasena incorrectos", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException cuentaDesactivada() {
        return new AuthException(
                "La cuenta se encuentra desactivada", HttpStatus.FORBIDDEN);
    }

    public static AuthException refreshTokenInvalido() {
        return new AuthException(
                "El refresh token es invalido, expiro o fue revocado", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException limitePerfiles(int maximo) {
        return new AuthException(
                "Alcanzaste el maximo de " + maximo + " perfiles por cuenta",
                HttpStatus.CONFLICT);
    }

    public static AuthException perfilDuplicado(String nombre) {
        return new AuthException(
                "Ya tienes un perfil llamado '" + nombre + "'", HttpStatus.CONFLICT);
    }

    public static AuthException perfilNoEncontrado() {
        return new AuthException("El perfil no existe", HttpStatus.NOT_FOUND);
    }

    public static AuthException usuarioNoEncontrado() {
        return new AuthException("El usuario no existe", HttpStatus.NOT_FOUND);
    }
}
