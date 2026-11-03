package cl.sugoianime.msauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de renovacion del access token")
public record RefreshRequest(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}
