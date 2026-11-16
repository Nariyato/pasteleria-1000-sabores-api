package cl.duoc.pasteleria.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 60)
    private String categoria;

    private LocalDate fecha;

    @Column(length = 120)
    private String autor;

    @Lob
    private String imagen;

    @Column(length = 500)
    private String resumen;

    /** Lista de párrafos, en orden. */
    @ElementCollection
    @CollectionTable(name = "blog_post_parrafos", joinColumns = @JoinColumn(name = "blog_post_id"))
    @OrderColumn(name = "orden")
    @Column(name = "parrafo", length = 2000)
    @Builder.Default
    private List<String> contenido = new ArrayList<>();
}
