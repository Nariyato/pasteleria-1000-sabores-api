package cl.duoc.pasteleria.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.BoletaRequest;
import cl.duoc.pasteleria.dto.BoletaResponse;
import cl.duoc.pasteleria.dto.EstadoRequest;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Boleta;
import cl.duoc.pasteleria.model.DatosDespacho;
import cl.duoc.pasteleria.model.DetalleBoleta;
import cl.duoc.pasteleria.model.EstadoBoleta;
import cl.duoc.pasteleria.model.Producto;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Tamano;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;
import cl.duoc.pasteleria.util.Descuentos;

/** Compra y consulta de boletas. Los precios, descuentos, stock y totales se calculan SIEMPRE aquí, desde la base de datos. */
@Service
@Transactional(readOnly = true)
public class BoletaService {

    private static final Set<Long> CATEGORIAS_DE_TORTA = Set.of(1L, 2L, 8L);

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, Clock reloj) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
    }

    @Transactional
    public BoletaResponse crear(String email, BoletaRequest datos) {
        Usuario usuario = buscarUsuario(email);
        LocalDate hoy = LocalDate.now(reloj);

        Boleta boleta = Boleta.builder()
                .usuario(usuario)
                .fecha(Instant.now(reloj))
                .estado(EstadoBoleta.PREPARACION)
                .costoDespacho(Descuentos.COSTO_DESPACHO)
                .datosDespacho(new DatosDespacho(
                        datos.despacho().nombre(),
                        datos.despacho().calle(),
                        datos.despacho().comuna(),
                        datos.despacho().fechaEntrega()))
                .build();

        List<Descuentos.Linea> lineas = new ArrayList<>();
        Map<String, Integer> pedidoPorProducto = new HashMap<>();

        for (BoletaRequest.Item item : datos.items()) {
            Producto producto = productoRepository
                    .findById(item.codigo())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + item.codigo()));

            if (!producto.isActivo()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre() + " no está disponible");
            }

            Tamano tamano = buscarTamano(producto, item.tamano());
            boolean hayMensaje = item.mensaje() != null && !item.mensaje().isBlank();

            if (hayMensaje && !producto.isPermiteMensaje()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre() + " no admite mensaje");
            }

            int precio = Descuentos.precioConOferta(
                    producto.getPrecio(),
                    tamano == null ? 0 : tamano.getExtra(),
                    producto.getOferta());
            boolean esTorta = producto.getForma() != null
                    || CATEGORIAS_DE_TORTA.contains(producto.getCategoria().getId());

            lineas.add(new Descuentos.Linea(precio, item.cantidad(), esTorta));
            pedidoPorProducto.merge(producto.getCodigo(), item.cantidad(), Integer::sum);

            boleta.agregarDetalle(DetalleBoleta.builder()
                    .producto(producto)
                    .nombre(producto.getNombre())
                    .cantidad(item.cantidad())
                    .precioUnitario(precio)
                    .subtotal(precio * item.cantidad())
                    .tamano(tamano == null ? null : tamano.getId())
                    .personas(tamano == null ? null : tamano.getPersonas())
                    .mensaje(hayMensaje ? item.mensaje().trim() : null)
                    .build());
        }

        descontarStock(pedidoPorProducto);

        Descuentos.Resultado resultado = Descuentos.calcular(
                lineas,
                usuario.getFechaNacimiento(),
                usuario.getCodigoDescuento(),
                esDuoc(usuario),
                tortaYaCanjeada(usuario, hoy),
                hoy);

        boleta.setSubtotal(resultado.subtotal());
        boleta.setDescuento(resultado.descuento());
        boleta.setDetalleDescuento(new ArrayList<>(resultado.detalle()));
        boleta.setTotal(resultado.total() + boleta.getCostoDespacho());

        Boleta guardada = boletaRepository.save(boleta);
        guardada.setNumero("B-" + String.format("%06d", guardada.getId()));
        return BoletaResponse.desde(guardada);
    }

    public List<BoletaResponse> listar() {
        return boletaRepository.findAll().stream().map(BoletaResponse::desde).toList();
    }

    public List<BoletaResponse> listarMias(String email) {
        Usuario usuario = buscarUsuario(email);
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()).stream()
                .map(BoletaResponse::desde)
                .toList();
    }

    public List<BoletaResponse> listarPorUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("No existe el usuario con id " + usuarioId);
        }
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(BoletaResponse::desde)
                .toList();
    }

    public BoletaResponse obtener(Long id, String email) {
        Boleta boleta = boletaRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta con id " + id));

        Usuario solicitante = buscarUsuario(email);
        if (solicitante.getRol() == Rol.CLIENTE && !boleta.getUsuario().getId().equals(solicitante.getId())) {
            throw new AccessDeniedException("La boleta pertenece a otro usuario");
        }
        return BoletaResponse.desde(boleta);
    }

    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoRequest datos) {
        Boleta boleta = boletaRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta con id " + id));
        boleta.setEstado(EstadoBoleta.desdeJson(datos.estado()));
        return BoletaResponse.desde(boleta);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + email));
    }

    private Tamano buscarTamano(Producto producto, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return producto.getTamanos().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El producto " + producto.getNombre() + " no tiene el tamaño " + id));
    }

    private void descontarStock(Map<String, Integer> pedidoPorProducto) {
        pedidoPorProducto.forEach((codigo, cantidad) -> {
            Producto p = productoRepository.findById(codigo).orElseThrow();
            if (p.getStock() < cantidad) {
                throw new ReglaNegocioException("No hay stock suficiente de " + p.getNombre());
            }
        });
        pedidoPorProducto.forEach((codigo, cantidad) -> {
            Producto p = productoRepository.findById(codigo).orElseThrow();
            p.setStock(p.getStock() - cantidad);
        });
    }

    private boolean esDuoc(Usuario usuario) {
        return usuario.getEmail().toLowerCase().endsWith("@duocuc.cl");
    }

    private boolean tortaYaCanjeada(Usuario usuario, LocalDate hoy) {
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()).stream()
                .filter(b -> b.getFecha().atZone(reloj.getZone()).getYear() == hoy.getYear())
                .anyMatch(b -> b.getDetalleDescuento().stream()
                        .anyMatch(x -> Descuentos.MOTIVO_TORTA_CUMPLE.equals(x.getMotivo())));
    }
}