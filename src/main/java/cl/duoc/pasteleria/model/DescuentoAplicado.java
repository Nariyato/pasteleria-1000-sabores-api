package cl.duoc.pasteleria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una línea del detalle de descuento de una boleta (en el front: detalleDescuento). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescuentoAplicado {

    @Column(nullable = false, length = 80)
    private String motivo;

    /** Monto descontado en pesos. */
    private int monto;
}
