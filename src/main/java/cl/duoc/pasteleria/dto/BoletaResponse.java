package cl.duoc.pasteleria.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.pasteleria.model.Boleta;
import cl.duoc.pasteleria.model.DatosDespacho;

/** Misma forma que la "orden" del front, para que las páginas no cambien. */
public record BoletaResponse(
        Long id,
        String numero,
        Long usuarioId,
        Cliente cliente,
        String estado,
        Instant fecha,
        List<Item> items,
        Despacho despacho,
        int subtotal,
        int descuento,
        List<Descuento> detalleDescuento,
        int costoDespacho,
        int total) {

    public record Cliente(String nombre, String email) {}

    public record Item(
            String codigo,
            String nombre,
            String imagen,
            int cantidad,
            int precio,
            String tamano,
            Integer personas,
            String mensaje) {}

    public record Despacho(String nombre, String calle, String comuna, LocalDate fechaEntrega) {}

    public record Descuento(String motivo, int monto) {}

    /** Debe llamarse dentro de la transacción: recorre relaciones LAZY. */
    public static BoletaResponse desde(Boleta b) {
        DatosDespacho dd = b.getDatosDespacho();
        boolean hayDespacho = dd != null && dd.getCalle() != null;
        return new BoletaResponse(
                b.getId(),
                b.getNumero(),
                b.getUsuario().getId(),
                new Cliente((b.getUsuario().getNombre() + " " + b.getUsuario().getApellidos()).trim(),
                        b.getUsuario().getEmail()),
                b.getEstado().aJson(),
                b.getFecha(),
                b.getDetalles().stream()
                        .map(d -> new Item(
                                d.getProducto().getCodigo(),
                                d.getNombre(),
                                d.getProducto().getImagen(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getTamano(),
                                d.getPersonas(),
                                d.getMensaje()))
                        .toList(),
                hayDespacho
                        ? new Despacho(dd.getNombre(), dd.getCalle(), dd.getComuna(), dd.getFechaEntrega())
                        : null,
                b.getSubtotal(),
                b.getDescuento(),
                b.getDetalleDescuento().stream()
                        .map(x -> new Descuento(x.getMotivo(), x.getMonto()))
                        .toList(),
                b.getCostoDespacho(),
                b.getTotal());
    }
}