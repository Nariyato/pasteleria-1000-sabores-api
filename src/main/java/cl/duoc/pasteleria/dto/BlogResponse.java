package cl.duoc.pasteleria.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cl.duoc.pasteleria.model.BlogPost;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que la API devuelve de una entrada del blog. Nunca se expone la entidad directamente. */
@Schema(description = "Entrada del blog")
public record BlogResponse(Long id, String titulo, String categoria, LocalDate fecha, String autor, String imagen,
        String resumen, List<String> contenido) {

    /** Debe llamarse dentro de la transacción: los párrafos son una colección que se carga de forma diferida. */
    public static BlogResponse desde(BlogPost post) {
        return new BlogResponse(post.getId(), post.getTitulo(), post.getCategoria(), post.getFecha(),
                post.getAutor(), post.getImagen(), post.getResumen(), new ArrayList<>(post.getContenido()));
    }
}
