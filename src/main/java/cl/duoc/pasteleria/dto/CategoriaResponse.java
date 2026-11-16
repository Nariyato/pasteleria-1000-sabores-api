package cl.duoc.pasteleria.dto;

import cl.duoc.pasteleria.model.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que la API devuelve de una categoría. Nunca se expone la entidad directamente. */
@Schema(description = "Categoría de productos")
public record CategoriaResponse(Long id, String nombre, String descripcion, String imagen) {

    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(),
                categoria.getImagen());
    }
}
