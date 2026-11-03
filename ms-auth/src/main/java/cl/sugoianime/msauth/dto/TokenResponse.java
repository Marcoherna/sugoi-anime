package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens emitidos tras una autenticacion exitosa (RNF-05)")
public record TokenResponse(

        @Schema(description = "JWT de acceso. Se envia en el header Authorization: Bearer <token>")
        String accessToken,

        @Schema(description = "Token opaco para renovar el acceso sin volver a autenticarse")
        String refreshToken,

        @Schema(example = "Bearer")
        String tipo,

        @Schema(description = "Segundos de vida del access token", example = "900")
        long expiraEnSegundos,

        @Schema(description = "Datos basicos del usuario autenticado")
        UsuarioDTO usuario
) {

    public static TokenResponse de(String access, String refresh,
                                   long segundos, UsuarioDTO usuario) {
        return new TokenResponse(access, refresh, "Bearer", segundos, usuario);
    }
}
