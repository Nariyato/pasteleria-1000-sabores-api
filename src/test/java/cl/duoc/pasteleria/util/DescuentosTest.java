package cl.duoc.pasteleria.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class DescuentosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    @Test
    void precioConOfertaRedondea() {
        assertEquals(40500, Descuentos.precioConOferta(45000, 0, 10));
        assertEquals(49500, Descuentos.precioConOferta(45000, 10000, 10));
        assertEquals(4400, Descuentos.precioConOferta(5500, 0, 20));
    }

    @Test
    void mayorDe50Gana50() {
        var r = Descuentos.calcular(
                List.of(new Descuentos.Linea(10000, 2, false)),
                LocalDate.of(1965, 3, 15),
                "FELICES50",
                false,
                false,
                HOY);
        assertEquals(10000, r.descuento());
        assertEquals(1, r.detalle().size());
    }

    @Test
    void felices10() {
        var r = Descuentos.calcular(
                List.of(new Descuentos.Linea(10000, 1, false)),
                LocalDate.of(1998, 7, 20),
                "FELICES50",
                false,
                false,
                HOY);
        assertEquals(1000, r.descuento());
    }

    @Test
    void tortaDuocAntesDelPorcentaje() {
        var lineas = List.of(
                new Descuentos.Linea(3000, 4, false),
                new Descuentos.Linea(45000, 1, true));
        var r = Descuentos.calcular(lineas, LocalDate.of(2004, 11, 16), "FELICES50", true, false, HOY);
        assertEquals(57000, r.subtotal());
        assertEquals(45000 + 1200, r.descuento());
        assertEquals(2, r.detalle().size());

        var r2 = Descuentos.calcular(lineas, LocalDate.of(2004, 11, 16), null, true, true, HOY);
        assertEquals(0, r2.descuento());
    }

    @Test
    void edadExactaDe50NoCalifica() {
        assertEquals(0, Descuentos.porcentaje(LocalDate.of(1976, 11, 16), null, HOY));
        assertEquals(50, Descuentos.porcentaje(LocalDate.of(1975, 11, 15), null, HOY));
    }
}