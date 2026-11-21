package cl.duoc.pasteleria.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo que envía el checkout. Nunca trae precios ni totales: el servidor los calcula. */
public record BoletaRequest(
        @NotEmpty(message = "La boleta necesita al menos un producto") @Valid List<Item> items,
        @NotNull(message = "Los datos de despacho son obligatorios") @Valid Despacho despacho) {

    public record Item(
            @NotBlank(message = "El código del producto es obligatorio") String codigo,
            @Min(value = 1, message = "La cantidad mínima es 1") int cantidad,
            String tamano,
            @Size(max = 100, message = "El mensaje no puede superar los 100 caracteres") String mensaje) {}

    public record Despacho(
            @NotBlank(message = "El nombre de quien recibe es obligatorio") String nombre,
            @NotBlank(message = "La calle es obligatoria") String calle,
            @NotBlank(message = "La comuna es obligatoria") String comuna,
            LocalDate fechaEntrega) {}
}