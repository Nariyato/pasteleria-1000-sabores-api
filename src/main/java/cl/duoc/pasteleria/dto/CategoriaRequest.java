package cl.duoc.pasteleria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que envía el cliente para crear o editar una categoría. */
@Schema(description = "Datos para crear o editar una categoría")
public record CategoriaRequest(

        @Schema(example = "Tortas Cuadradas")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @Schema(example = "Tortas cuadradas para compartir.")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        @Schema(example = "/img/Torta cuadrada de chocolate.jpg")
        String imagen) {
}
