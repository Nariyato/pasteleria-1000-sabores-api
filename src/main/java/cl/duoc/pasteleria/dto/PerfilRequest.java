package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Datos editables del perfil del usuario autenticado")
public record PerfilRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        String direccion,

        String comuna,

        String region,

        @Pattern(regexp = "^$|^\\+?\\d{8,12}$", message = "El teléfono debe tener entre 8 y 12 dígitos")
        String telefono,

        @Schema(description = "Envía null para quitar la foto")
        String foto) {
}