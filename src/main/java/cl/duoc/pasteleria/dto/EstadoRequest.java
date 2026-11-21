package cl.duoc.pasteleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
                @Pattern(regexp = "preparacion|en_camino|entregado", message = "Estado no válido")
                String estado) {}