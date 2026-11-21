package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens de sesión y datos del usuario")
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario) {
}