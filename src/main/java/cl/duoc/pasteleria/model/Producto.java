package cl.duoc.pasteleria.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    /** Código del producto (TC001, PI002...). Es el identificador, igual que en el front. */
    @Id
    @Column(length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Precio base en pesos chilenos. */
    @Column(nullable = false)
    private int precio;

    @Column(nullable = false)
    private int stock;

    /** Porcentaje de oferta (0 = sin oferta). */
    @Column(nullable = false)
    private int oferta;

    @Column(nullable = false)
    private boolean destacado;

    @Column(nullable = false)
    private boolean activo;

    /** cuadrada, circular o null si el producto no es una torta. */
    @Column(length = 20)
    private String forma;

    @Column(nullable = false)
    private boolean permiteMensaje;

    @Lob
    private String imagen;

    @ElementCollection
    @CollectionTable(name = "producto_tamanos", joinColumns = @JoinColumn(name = "producto_codigo"))
    @Builder.Default
    private List<Tamano> tamanos = new ArrayList<>();
}
