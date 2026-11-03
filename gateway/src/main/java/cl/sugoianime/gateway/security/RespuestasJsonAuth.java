package cl.sugoianime.gateway.security;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * Respuestas de error en JSON.
 *
 * Por defecto Spring Security devuelve 401 y 403 con el cuerpo vacio.
 * Un frontend que recibe una respuesta sin cuerpo no puede distinguir
 * "token expirado" de "sin permisos" ni mostrar un mensaje util.
 *
 * NOTA DE SEGURIDAD (Ley 21.459): los mensajes son deliberadamente
 * genericos. No se revela si el token expiro, si la firma es invalida o
 * si el emisor no coincide: ese detalle solo ayudaria a un atacante a
 * afinar su intento. El detalle real queda en los logs del servidor.
 */
@Component
public class RespuestasJsonAuth {

    public ServerAuthenticationEntryPoint entryPoint401() {
        return (exchange, ex) -> escribir(
                exchange.getResponse(),
                HttpStatus.UNAUTHORIZED,
                "No autenticado",
                "Se requiere un token de acceso valido para esta operacion",
                exchange.getRequest().getPath().value());
    }

    public ServerAccessDeniedHandler accessDenied403() {
        return (exchange, ex) -> escribir(
                exchange.getResponse(),
                HttpStatus.FORBIDDEN,
                "Acceso denegado",
                "Tu cuenta no tiene permisos suficientes para esta operacion",
                exchange.getRequest().getPath().value());
    }

    private Mono<Void> escribir(ServerHttpResponse response, HttpStatus estado,
                                String error, String mensaje, String ruta) {

        response.setStatusCode(estado);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String cuerpo = """
            {"momento":"%s","estado":%d,"error":"%s","mensaje":"%s","ruta":"%s"}"""
                .formatted(OffsetDateTime.now(), estado.value(), error, mensaje, ruta);

        DataBuffer buffer = response.bufferFactory()
                .wrap(cuerpo.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
