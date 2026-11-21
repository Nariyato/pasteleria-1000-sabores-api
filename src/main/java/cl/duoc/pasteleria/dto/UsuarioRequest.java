package cl.duoc.pasteleria.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o editar un usuario (solo administrador)")
public record UsuarioRequest(

        @NotBlank(message = "El RUN es obligatorio")
        @Size(max = 12, message = "El RUN no puede superar los 12 caracteres")
        String run,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String email,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser del pasado")
        LocalDate fechaNacimiento,

        @Schema(description = "Obligatoria al crear; al editar, vacía o ausente no la cambia")
        @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres")
        String password,

        @Schema(example = "FELICES50")
        String codigoDescuento,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "^(admin|vendedor|cliente)$", message = "El rol debe ser admin, vendedor o cliente")
        String rol,

        String direccion,

        String comuna,

        String region,

        @Pattern(regexp = "^$|^\\+?\\d{8,12}$", message = "El teléfono debe tener entre 8 y 12 dígitos")
        String telefono,

        String foto) {
}