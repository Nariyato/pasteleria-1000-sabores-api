package cl.duoc.pasteleria.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import cl.duoc.pasteleria.model.DescuentoAplicado;

/** Reglas de descuento portadas desde src/utils/descuentos.js del front. Clase pura: sin base de datos, fácil de probar. */
public final class Descuentos {

    public static final String CODIGO_FELICES = "FELICES50";
    public static final String MOTIVO_TORTA_CUMPLE = "Torta gratis por cumpleaños Duoc";
    public static final String MOTIVO_MAYOR_50 = "Mayor de 50 años";
    public static final String MOTIVO_FELICES = "Código FELICES50";
    public static final int COSTO_DESPACHO = 3500;

    private Descuentos() {}

    /** Una línea ya valorizada, con lo mínimo que necesitan las reglas. */
    public record Linea(int precioUnitario, int cantidad, boolean esTorta) {}

    public record Resultado(int subtotal, int descuento, List<DescuentoAplicado> detalle) {
        public int total() {
            return subtotal - descuento;
        }
    }

    public static int edad(LocalDate nacimiento, LocalDate hoy) {
        return Period.between(nacimiento, hoy).getYears();
    }

    public static boolean esCumpleanos(LocalDate nacimiento, LocalDate hoy) {
        return nacimiento != null && nacimiento.getMonth() == hoy.getMonth()
                && nacimiento.getDayOfMonth() == hoy.getDayOfMonth();
    }

    /** precioConOferta del front: (precio + extra del tamaño) con la oferta, redondeado. */
    public static int precioConOferta(int precio, int extra, int oferta) {
        return (int) Math.round((precio + extra) * (1 - oferta / 100.0));
    }

    /** Porcentaje que gana (no se suman): 0, 10 (FELICES50) o 50 (mayor de 50). */
    public static int porcentaje(LocalDate nacimiento, String codigo, LocalDate hoy) {
        int mayor = 0;
        if (nacimiento != null && edad(nacimiento, hoy) > 50) {
            mayor = 50;
        }
        if (CODIGO_FELICES.equals(codigo)) {
            mayor = Math.max(mayor, 10);
        }
        return mayor;
    }

    public static Resultado calcular(List<Linea> lineas, LocalDate nacimiento, String codigo, boolean esDuoc,
            boolean tortaYaCanjeada, LocalDate hoy) {
        int subtotal = lineas.stream().mapToInt(l -> l.precioUnitario() * l.cantidad()).sum();
        List<DescuentoAplicado> detalle = new ArrayList<>();
        int descuento = 0;

        if (esDuoc && esCumpleanos(nacimiento, hoy) && !tortaYaCanjeada) {
            lineas.stream().filter(Linea::esTorta).findFirst().ifPresent(torta -> detalle
                    .add(new DescuentoAplicado(MOTIVO_TORTA_CUMPLE, torta.precioUnitario())));
        }
        descuento = detalle.stream().mapToInt(DescuentoAplicado::getMonto).sum();

        int pct = porcentaje(nacimiento, codigo, hoy);
        if (pct > 0) {
            int monto = (int) Math.round((subtotal - descuento) * pct / 100.0);
            detalle.add(new DescuentoAplicado(pct == 50 ? MOTIVO_MAYOR_50 : MOTIVO_FELICES, monto));
            descuento += monto;
        }

        return new Resultado(subtotal, descuento, detalle);
    }
}