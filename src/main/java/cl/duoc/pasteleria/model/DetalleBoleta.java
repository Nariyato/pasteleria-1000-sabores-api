package cl.duoc.pasteleria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una línea de la boleta: cuántas unidades de un producto se compraron y a qué precio. */
@Entity
@Table(name = "detalles_boleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleBoleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_codigo", nullable = false)
    private Producto producto;

    /** Nombre del producto al momento de la compra (si el producto cambia, la boleta no). */
    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int cantidad;

    /** Precio unitario pagado (ya con oferta y recargo de tamaño). */
    @Column(nullable = false)
    private int precioUnitario;

    @Column(nullable = false)
    private int subtotal;

    @Column(length = 20)
    private String tamano;

    private Integer personas;

    /** Mensaje en la torta, si el cliente lo pidió. */
    @Column(length = 100)
    private String mensaje;
}
