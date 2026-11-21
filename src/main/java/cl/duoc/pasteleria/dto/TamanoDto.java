package cl.duoc.pasteleria.dto;

import cl.duoc.pasteleria.model.Tamano;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Tamaño de torta (chica, mediana, grande)")
public record TamanoDto(

        @Schema(example = "mediana")
        @NotBlank(message = "El id del tamaño es obligatorio")
        String id,

        @Schema(example = "15")
        @Min(value = 1, message = "Las personas deben ser al menos 1")
        int personas,

        @Schema(example = "10000")
        @Min(value = 0, message = "El recargo no puede ser negativo")
        int extra) {

    public static TamanoDto desde(Tamano tamano) {
        return new TamanoDto(tamano.getId(), tamano.getPersonas(), tamano.getExtra());
    }

    public Tamano aEntidad() {
        return new Tamano(id, personas, extra);
    }
}