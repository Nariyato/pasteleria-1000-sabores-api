package cl.duoc.pasteleria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tamaño de una torta (chica, mediana, grande). No es una tabla propia: se guarda dentro de Producto. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tamano {

    @Column(nullable = false, length = 20)
    private String id;

    private int personas;

    /** Recargo en pesos que se suma al precio base. */
    private int extra;
}
