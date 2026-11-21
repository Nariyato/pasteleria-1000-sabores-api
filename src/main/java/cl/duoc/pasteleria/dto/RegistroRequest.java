package cl.duoc.pasteleria.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrarse como cliente")
public record RegistroRequest(

        @Schema(example = "123456785")
        @NotBlank(message = "El RUN es obligatorio")
        @Size(max = 12, message = "El RUN no puede superar los 12 caracteres")
        String run,

        @Schema(example = "María")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @Schema(example = "González Pérez")
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres")
        String apellidos,

        @Schema(example = "maria@gmail.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String email,

        @Schema(example = "1990-05-15")
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser del pasado")
        LocalDate fechaNacimiento,

        @Schema(example = "1234")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres")
        String password,

        @Schema(example = "FELICES50")
        String codigoDescuento) {
}