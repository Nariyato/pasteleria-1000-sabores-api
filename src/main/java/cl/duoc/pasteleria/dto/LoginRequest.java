package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesión")
public record LoginRequest(

        @Schema(example = "rosa@gmail.com")
        @NotBlank(message = "El correo es obligatorio")
        String email,

        @Schema(example = "1234")
        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}