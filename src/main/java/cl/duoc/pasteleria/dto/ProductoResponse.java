package cl.duoc.pasteleria.dto;

import java.util.List;

import cl.duoc.pasteleria.model.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto del catálogo")
public record ProductoResponse(

        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        int precio,
        int stock,
        int oferta,
        boolean destacado,
        boolean activo,
        String forma,
        boolean permiteMensaje,
        String imagen,
        List<TamanoDto> tamanos) {

    public static ProductoResponse desde(Producto producto) {
        List<TamanoDto> tamanos = producto.getTamanos() == null
                ? List.of()
                : producto.getTamanos().stream().map(TamanoDto::desde).toList();
        return new ProductoResponse(
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria().getId(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getOferta(),
                producto.isDestacado(),
                producto.isActivo(),
                producto.getForma(),
                producto.isPermiteMensaje(),
                producto.getImagen(),
                tamanos);
    }
}