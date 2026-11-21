package cl.duoc.pasteleria.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o editar un producto")
public record ProductoRequest(

        @Schema(example = "TC001")
        @Pattern(regexp = "^[A-Z]{2,3}\\d{3}$", message = "El código debe tener el formato de 2-3 letras y 3 dígitos")
        String codigo,

        @Schema(example = "Torta cuadrada de chocolate")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(example = "Bizcocho de chocolate con crema")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @Schema(example = "1")
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Schema(example = "25000")
        @Min(value = 1, message = "El precio debe ser al menos 1")
        int precio,

        @Schema(example = "10")
        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock,

        @Schema(example = "0")
        @Min(value = 0, message = "La oferta no puede ser negativa")
        @Max(value = 90, message = "La oferta no puede superar el 90 %")
        int oferta,

        @Schema(example = "true")
        boolean destacado,

        @Schema(example = "true")
        boolean activo,

        @Schema(example = "cuadrada")
        @Pattern(regexp = "^(cuadrada|circular)$", message = "La forma debe ser cuadrada o circular")
        String forma,

        @Schema(example = "true")
        boolean permiteMensaje,

        @Schema(example = "/img/Torta cuadrada de chocolate.jpg")
        String imagen,

        @Valid
        List<TamanoDto> tamanos) {
}