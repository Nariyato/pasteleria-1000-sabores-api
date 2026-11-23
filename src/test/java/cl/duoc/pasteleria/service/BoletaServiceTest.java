package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import cl.duoc.pasteleria.dto.BoletaRequest;
import cl.duoc.pasteleria.dto.BoletaResponse;
import cl.duoc.pasteleria.dto.EstadoRequest;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Boleta;
import cl.duoc.pasteleria.model.Categoria;
import cl.duoc.pasteleria.model.DescuentoAplicado;
import cl.duoc.pasteleria.model.EstadoBoleta;
import cl.duoc.pasteleria.model.Producto;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Tamano;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;
import cl.duoc.pasteleria.util.Descuentos;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Santiago"));

    @Mock
    private BoletaRepository boletaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private BoletaService boletaService;

    @BeforeEach
    void configurar() {
        boletaService = new BoletaService(boletaRepository, productoRepository, usuarioRepository, RELOJ);
    }

    private Usuario cliente(String email, LocalDate nacimiento, String codigo, Rol rol) {
        return Usuario.builder()
                .id(1L)
                .nombre("Test")
                .apellidos("User")
                .email(email)
                .fechaNacimiento(nacimiento)
                .codigoDescuento(codigo)
                .rol(rol)
                .build();
    }

    private Producto producto(String codigo, int precio, int stock, int oferta, Categoria cat, String forma,
            boolean activo, boolean permiteMensaje, List<Tamano> tamanos) {
        return Producto.builder()
                .codigo(codigo)
                .nombre("Prod " + codigo)
                .categoria(cat)
                .precio(precio)
                .stock(stock)
                .oferta(oferta)
                .forma(forma)
                .activo(activo)
                .permiteMensaje(permiteMensaje)
                .imagen("/img/x.jpg")
                .tamanos(tamanos != null ? tamanos : new ArrayList<>())
                .build();
    }

    private BoletaRequest requestSimple(String codigo, int cantidad) {
        return new BoletaRequest(
                List.of(new BoletaRequest.Item(codigo, cantidad, null, null)),
                new BoletaRequest.Despacho("Ana", "Calle 1", "Maipú", HOY.plusDays(1)));
    }

    private Boleta boletaMinima(long id, Usuario titular) {
        return Boleta.builder()
                .id(id)
                .numero("B-" + String.format("%06d", id))
                .usuario(titular)
                .estado(EstadoBoleta.PREPARACION)
                .fecha(Instant.parse("2026-01-01T12:00:00Z"))
                .subtotal(5000)
                .descuento(0)
                .costoDespacho(Descuentos.COSTO_DESPACHO)
                .total(5000 + Descuentos.COSTO_DESPACHO)
                .detalles(new ArrayList<>())
                .detalleDescuento(new ArrayList<>())
                .build();
    }

    @Test
    void compraSinDescuentosSumaDespacho() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto empanada = producto("PT001", 3000, 10, 0, cat, null, true, false, null);
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(rosa.getEmail())).thenReturn(Optional.of(rosa));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(empanada));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(3L);
            return b;
        });

        BoletaResponse r = boletaService.crear(rosa.getEmail(), requestSimple("PT001", 2));

        assertThat(r.subtotal()).isEqualTo(6000);
        assertThat(r.descuento()).isZero();
        assertThat(r.costoDespacho()).isEqualTo(Descuentos.COSTO_DESPACHO);
        assertThat(r.total()).isEqualTo(6000 + Descuentos.COSTO_DESPACHO);
        assertThat(r.numero()).isEqualTo("B-000003");
        assertThat(r.estado()).isEqualTo("preparacion");
        assertThat(empanada.getStock()).isEqualTo(8);
    }

    @Test
    void rosaMayorDe50Aplica50Porciento() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 10000, 5, 0, cat, null, true, false, null);
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1965, 3, 15), "50%", Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(rosa.getEmail())).thenReturn(Optional.of(rosa));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        BoletaResponse r = boletaService.crear(rosa.getEmail(), requestSimple("PT001", 2));

        assertThat(r.descuento()).isEqualTo(10000);
    }

    @Test
    void pedroFelices50Aplica10Porciento() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 10000, 5, 0, cat, null, true, false, null);
        Usuario pedro = cliente("pedro@gmail.com", LocalDate.of(1998, 1, 1), "FELICES50", Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(pedro.getEmail())).thenReturn(Optional.of(pedro));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(11L);
            return b;
        });

        BoletaResponse r = boletaService.crear(pedro.getEmail(), requestSimple("PT001", 1));

        assertThat(r.descuento()).isEqualTo(1000);
    }

    @Test
    void camilaCumpleTortaGratisAntesDelPorcentaje() {
        Categoria tortas = Categoria.builder().id(1L).nombre("Tortas").build();
        Categoria pasteleria = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto torta = producto("TC001", 45000, 5, 0, tortas, "circular", true, true,
                List.of(new Tamano("chica", 8, 0)));
        Producto empanada = producto("PT001", 3000, 20, 0, pasteleria, null, true, false, null);
        Usuario camila = cliente("camila@duocuc.cl", LocalDate.of(2004, 11, 16), "FELICES50", Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(camila.getEmail())).thenReturn(Optional.of(camila));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(empanada));
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(torta));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(12L);
            return b;
        });

        var datos = new BoletaRequest(
                List.of(
                        new BoletaRequest.Item("PT001", 4, null, null),
                        new BoletaRequest.Item("TC001", 1, "chica", null)),
                new BoletaRequest.Despacho("Camila", "Calle", "Santiago", HOY));

        BoletaResponse r = boletaService.crear(camila.getEmail(), datos);

        assertThat(r.subtotal()).isEqualTo(57000);
        assertThat(r.descuento()).isEqualTo(45000 + 1200);
        assertThat(r.detalleDescuento()).hasSize(2);
    }

    @Test
    void camilaYaCanjeoTortaEsteAnio() {
        Categoria tortas = Categoria.builder().id(1L).nombre("Tortas").build();
        Producto torta = producto("TC001", 45000, 5, 0, tortas, "circular", true, true,
                List.of(new Tamano("chica", 8, 0)));
        Usuario camila = cliente("camila@duocuc.cl", LocalDate.of(2004, 11, 16), null, Rol.CLIENTE);

        Boleta anterior = Boleta.builder()
                .fecha(Instant.parse("2026-03-01T12:00:00Z"))
                .detalleDescuento(List.of(new DescuentoAplicado(Descuentos.MOTIVO_TORTA_CUMPLE, 45000)))
                .build();

        when(usuarioRepository.findByEmailIgnoreCase(camila.getEmail())).thenReturn(Optional.of(camila));
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(torta));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of(anterior));
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(13L);
            return b;
        });

        BoletaResponse r = boletaService.crear(
                camila.getEmail(),
                new BoletaRequest(
                        List.of(new BoletaRequest.Item("TC001", 1, "chica", null)),
                        new BoletaRequest.Despacho("Camila", "Calle", "Santiago", HOY)));

        assertThat(r.detalleDescuento()).noneMatch(d -> Descuentos.MOTIVO_TORTA_CUMPLE.equals(d.motivo()));
    }

    @Test
    void stockInsuficienteNoDescuenta() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 3000, 1, 0, cat, null, true, false, null);
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> boletaService.crear(u.getEmail(), requestSimple("PT001", 2)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(p.getStock()).isEqualTo(1);
        verify(boletaRepository, never()).save(any());
    }

    @Test
    void productoInactivo409() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 3000, 5, 0, cat, null, false, false, null);
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> boletaService.crear(u.getEmail(), requestSimple("PT001", 1)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void productoInexistente404() {
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);
        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boletaService.crear(u.getEmail(), requestSimple("XXX", 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void tamanoInvalido409() {
        Categoria tortas = Categoria.builder().id(1L).nombre("Tortas").build();
        Producto torta = producto("TC001", 45000, 5, 0, tortas, "circular", true, true,
                List.of(new Tamano("chica", 8, 0)));
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(torta));

        var datos = new BoletaRequest(
                List.of(new BoletaRequest.Item("TC001", 1, "enorme", null)),
                new BoletaRequest.Despacho("Ana", "Calle", "Maipú", HOY));
        assertThatThrownBy(() -> boletaService.crear(u.getEmail(), datos)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void mensajeEnProductoSinMensaje409() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 3000, 5, 0, cat, null, true, false, null);
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));

        var datos = new BoletaRequest(
                List.of(new BoletaRequest.Item("PT001", 1, null, "Feliz día")),
                new BoletaRequest.Despacho("Ana", "Calle", "Maipú", HOY));
        assertThatThrownBy(() -> boletaService.crear(u.getEmail(), datos)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void mismoProductoEnDosLineasSumaStock() {
        Categoria cat = Categoria.builder().id(4L).nombre("Pastelería").build();
        Producto p = producto("PT001", 3000, 10, 0, cat, null, true, false, null);
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("PT001")).thenReturn(Optional.of(p));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(20L);
            return b;
        });

        var datos = new BoletaRequest(
                List.of(
                        new BoletaRequest.Item("PT001", 2, null, null),
                        new BoletaRequest.Item("PT001", 3, null, null)),
                new BoletaRequest.Despacho("Ana", "Calle", "Maipú", HOY));
        boletaService.crear(u.getEmail(), datos);

        assertThat(p.getStock()).isEqualTo(5);
    }

    @Test
    void clienteNoVeBoletaAjena() {
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);
        Usuario camila = cliente("camila@duocuc.cl", LocalDate.of(2004, 11, 16), null, Rol.CLIENTE);
        camila.setId(2L);

        Boleta ajena = Boleta.builder().id(1L).usuario(camila).estado(EstadoBoleta.PREPARACION).build();

        when(boletaRepository.findById(1L)).thenReturn(Optional.of(ajena));
        when(usuarioRepository.findByEmailIgnoreCase(rosa.getEmail())).thenReturn(Optional.of(rosa));

        assertThatThrownBy(() -> boletaService.obtener(1L, rosa.getEmail())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listarPorUsuarioInexistente404() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> boletaService.listarPorUsuario(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstadoActualizaBoleta() {
        Usuario admin = cliente("admin@duoc.cl", LocalDate.of(1980, 1, 1), null, Rol.ADMIN);
        Boleta boleta = Boleta.builder()
                .id(1L)
                .numero("B-000001")
                .usuario(admin)
                .estado(EstadoBoleta.PREPARACION)
                .fecha(Instant.parse("2026-01-01T12:00:00Z"))
                .subtotal(1000)
                .descuento(0)
                .costoDespacho(Descuentos.COSTO_DESPACHO)
                .total(1000 + Descuentos.COSTO_DESPACHO)
                .detalles(new ArrayList<>())
                .detalleDescuento(new ArrayList<>())
                .build();

        when(boletaRepository.findById(1L)).thenReturn(Optional.of(boleta));

        BoletaResponse r = boletaService.cambiarEstado(1L, new EstadoRequest("en_camino"));

        assertThat(r.estado()).isEqualTo("en_camino");
        assertThat(boleta.getEstado()).isEqualTo(EstadoBoleta.EN_CAMINO);
    }

    @Test
    void precioConOfertaYTamanoDesdeBase() {
        Categoria tortas = Categoria.builder().id(1L).nombre("Tortas").build();
        Producto torta = producto("TC001", 45000, 5, 10, tortas, "circular", true, true,
                List.of(new Tamano("mediana", 15, 10000)));
        Usuario u = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);

        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(torta));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of());
        when(boletaRepository.save(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(30L);
            return b;
        });

        var datos = new BoletaRequest(
                List.of(new BoletaRequest.Item("TC001", 1, "mediana", null)),
                new BoletaRequest.Despacho("Ana", "Calle", "Maipú", HOY));
        BoletaResponse r = boletaService.crear(u.getEmail(), datos);

        assertThat(r.items().get(0).precio()).isEqualTo(Descuentos.precioConOferta(45000, 10000, 10));
        ArgumentCaptor<Boleta> captor = ArgumentCaptor.forClass(Boleta.class);
        verify(boletaRepository).save(captor.capture());
        assertThat(captor.getValue().getDetalles().get(0).getPrecioUnitario())
                .isEqualTo(Descuentos.precioConOferta(45000, 10000, 10));
    }

    @Test
    void listarDevuelveTodasLasBoletas() {
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);
        Usuario camila = cliente("camila@duocuc.cl", LocalDate.of(2004, 11, 16), null, Rol.CLIENTE);
        camila.setId(2L);
        Boleta b1 = boletaMinima(1L, rosa);
        Boleta b2 = boletaMinima(2L, camila);

        when(boletaRepository.findAll()).thenReturn(List.of(b1, b2));

        List<BoletaResponse> lista = boletaService.listar();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).numero()).isEqualTo("B-000001");
        assertThat(lista.get(1).cliente().email()).isEqualTo("camila@duocuc.cl");
    }

    @Test
    void listarMiasSoloDelUsuarioAutenticado() {
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);
        Boleta propia = boletaMinima(5L, rosa);

        when(usuarioRepository.findByEmailIgnoreCase(rosa.getEmail())).thenReturn(Optional.of(rosa));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of(propia));

        List<BoletaResponse> lista = boletaService.listarMias(rosa.getEmail());

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).id()).isEqualTo(5L);
    }

    @Test
    void clienteVeSuPropiaBoleta() {
        Usuario rosa = cliente("rosa@gmail.com", LocalDate.of(1990, 1, 1), null, Rol.CLIENTE);
        Boleta propia = boletaMinima(7L, rosa);

        when(boletaRepository.findById(7L)).thenReturn(Optional.of(propia));
        when(usuarioRepository.findByEmailIgnoreCase(rosa.getEmail())).thenReturn(Optional.of(rosa));

        BoletaResponse r = boletaService.obtener(7L, rosa.getEmail());

        assertThat(r.id()).isEqualTo(7L);
        assertThat(r.cliente().email()).isEqualTo(rosa.getEmail());
    }

    @Test
    void obtenerBoletaInexistente404() {
        when(boletaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boletaService.obtener(404L, "rosa@gmail.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstadoBoletaInexistente404() {
        when(boletaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boletaService.cambiarEstado(99L, new EstadoRequest("entregado")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConUsuarioInexistente404() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@test.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boletaService.crear("nadie@test.cl", requestSimple("PT001", 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(boletaRepository, never()).save(any());
    }
}