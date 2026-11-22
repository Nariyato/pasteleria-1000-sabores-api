package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que envía el formulario de contacto. No trae usuarioId: si hay sesión, el backend lo saca del token. */
@Schema(description = "Datos del formulario de contacto")
public record MensajeRequest(

        @Schema(example = "Reclamo")
        @NotBlank(message = "El tipo de consulta es obligatorio")
        @Size(max = 60, message = "El tipo no puede superar los 60 caracteres")
        String tipo,

        @Schema(example = "Rosa Muñoz")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @Schema(example = "rosa@gmail.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String email,

        @Schema(example = "Mi torta llegó tarde")
        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
        String mensaje) {
}
