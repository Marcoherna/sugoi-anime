package cl.sugoianime.msinteracciones.security;

import cl.sugoianime.msinteracciones.client.AuthClient;
import cl.sugoianime.msinteracciones.client.dto.PerfilExterno;
import cl.sugoianime.msinteracciones.exception.InteraccionException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifica que un perfil pertenezca al usuario autenticado.
 *
 * POR QUE HACE FALTA: el perfil vive en ms-auth, en otra base de datos.
 * Sin esta comprobacion, cualquier usuario podria pasar perfilId=999 y
 * leer o modificar los favoritos de otra persona. Es la defensa contra
 * IDOR (Insecure Direct Object Reference) en un contexto distribuido.
 *
 * La lista de perfiles se cachea 60 segundos en AuthClient, asi que
 * esta validacion NO implica una llamada de red en cada peticion.
 */
@Component
public class PerfilGuard {

    private final AuthClient authClient;

    public PerfilGuard(AuthClient authClient) {
        this.authClient = authClient;
    }

    public void verificarPropiedad(Jwt jwt, Long perfilId) {
        if (perfilId == null) {
            throw InteraccionException.perfilNoPertenece();
        }

        List<PerfilExterno> perfiles = authClient.listarPerfiles(jwt.getTokenValue());

        boolean pertenece = perfiles.stream()
                .anyMatch(p -> perfilId.equals(p.id()));

        if (!pertenece) {
            throw InteraccionException.perfilNoPertenece();
        }
    }

    public Long usuarioIdDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
