package cl.duoc.pasteleria.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos de despacho que se piden en el checkout. Se guardan dentro de la tabla boletas. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatosDespacho {

    @Column(name = "despacho_nombre", length = 120)
    private String nombre;

    @Column(name = "despacho_calle", length = 150)
    private String calle;

    @Column(name = "despacho_comuna", length = 60)
    private String comuna;

    @Column(name = "despacho_fecha_entrega")
    private LocalDate fechaEntrega;
}
