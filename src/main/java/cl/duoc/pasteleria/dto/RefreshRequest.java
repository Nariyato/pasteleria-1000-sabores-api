package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token de renovación de sesión")
public record RefreshRequest(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken) {
}