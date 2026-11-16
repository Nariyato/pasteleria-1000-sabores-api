package cl.duoc.pasteleria.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Boleta de compra. En el front se llamaba "orden". */
@Entity
@Table(name = "boletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número visible para el cliente, por ejemplo B-000001. */
    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBoleta estado;

    @Column(nullable = false)
    private int subtotal;

    @Column(nullable = false)
    private int descuento;

    @Column(nullable = false)
    private int costoDespacho;

    @Column(nullable = false)
    private int total;

    @Embedded
    private DatosDespacho datosDespacho;

    @ElementCollection
    @CollectionTable(name = "boleta_descuentos", joinColumns = @JoinColumn(name = "boleta_id"))
    @Builder.Default
    private List<DescuentoAplicado> detalleDescuento = new ArrayList<>();

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleBoleta> detalles = new ArrayList<>();

    /** Agrega un detalle y lo enlaza con esta boleta (mantiene los dos lados de la relación). */
    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }
}
