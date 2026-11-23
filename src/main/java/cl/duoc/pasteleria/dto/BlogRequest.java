package cl.duoc.pasteleria.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que envía el administrador para crear o editar una entrada del blog. */
@Schema(description = "Datos para crear o editar una entrada del blog")
public record BlogRequest(

        @Schema(example = "Cómo lograr un bizcocho esponjoso")
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String titulo,

        @Schema(example = "Consejos de maestros")
        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 60, message = "La categoría no puede superar los 60 caracteres")
        String categoria,

        @Schema(example = "2026-11-18")
        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @Schema(example = "Freddy Camposo, Jefe de Horno")
        @Size(max = 120, message = "El autor no puede superar los 120 caracteres")
        String autor,

        @Schema(example = "/img/amasando.jpg")
        String imagen,

        @Schema(example = "Los pasos básicos para que tu bizcocho no se baje.")
        @Size(max = 500, message = "El resumen no puede superar los 500 caracteres")
        String resumen,

        @Schema(example = "[\"Primer párrafo del artículo.\", \"Segundo párrafo del artículo.\"]")
        @NotEmpty(message = "El contenido debe tener al menos un párrafo")
        List<@NotBlank(message = "Los párrafos no pueden estar vacíos")
             @Size(max = 2000, message = "Cada párrafo no puede superar los 2000 caracteres") String> contenido) {
}
