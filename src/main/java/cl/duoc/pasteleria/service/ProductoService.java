package cl.duoc.pasteleria.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.ProductoRequest;
import cl.duoc.pasteleria.dto.ProductoResponse;
import cl.duoc.pasteleria.dto.TamanoDto;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Categoria;
import cl.duoc.pasteleria.model.Producto;
import cl.duoc.pasteleria.repository.CategoriaRepository;
import cl.duoc.pasteleria.repository.DetalleBoletaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    public static final int STOCK_CRITICO = 5;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
            DetalleBoletaRepository detalleBoletaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
    }

    public List<ProductoResponse> buscar(String texto, Long categoriaId, String forma, Integer personas,
            Integer precioMin, Integer precioMax) {
        return filtrarActivos(productoRepository.findByActivoTrue(), texto, categoriaId, forma, personas, precioMin,
                precioMax);
    }

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .sorted(Comparator.comparing(Producto::getCodigo))
                .map(ProductoResponse::desde)
                .toList();
    }

    public List<ProductoResponse> listarDestacados() {
        return productoRepository.findByActivoTrueAndDestacadoTrue().stream()
                .sorted(Comparator.comparing(Producto::getCodigo))
                .map(ProductoResponse::desde)
                .toList();
    }

    public List<ProductoResponse> listarOfertas() {
        return productoRepository.findByActivoTrueAndOfertaGreaterThanOrderByOfertaDesc(0).stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    public List<ProductoResponse> listarCriticos() {
        return productoRepository.findByStockLessThan(STOCK_CRITICO).stream()
                .sorted(Comparator.comparing(Producto::getCodigo))
                .map(ProductoResponse::desde)
                .toList();
    }

    public ProductoResponse obtenerPorCodigo(String codigo) {
        return ProductoResponse.desde(buscar(codigo));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest datos) {
        if (productoRepository.existsById(datos.codigo())) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + datos.codigo());
        }
        Categoria categoria = buscarCategoria(datos.categoriaId());
        Producto producto = aplicarDatos(Producto.builder().codigo(datos.codigo()).build(), datos, categoria);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(String codigo, ProductoRequest datos) {
        Producto producto = buscar(codigo);
        Categoria categoria = buscarCategoria(datos.categoriaId());
        aplicarDatos(producto, datos, categoria);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(String codigo) {
        Producto producto = buscar(codigo);
        if (detalleBoletaRepository.existsByProducto_Codigo(codigo)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el producto " + producto.getNombre() + " porque tiene ventas registradas");
        }
        productoRepository.delete(producto);
    }

    private List<ProductoResponse> filtrarActivos(List<Producto> productos, String texto, Long categoriaId,
            String forma, Integer personas, Integer precioMin, Integer precioMax) {
        return productos.stream()
                .filter(p -> texto == null || texto.isBlank()
                        || p.getNombre().toLowerCase(Locale.ROOT).contains(texto.toLowerCase(Locale.ROOT)))
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .filter(p -> forma == null || forma.isBlank() || forma.equals(p.getForma()))
                .filter(p -> personas == null || tieneCapacidad(p, personas))
                .filter(p -> precioMin == null || p.getPrecio() >= precioMin)
                .filter(p -> precioMax == null || p.getPrecio() <= precioMax)
                .sorted(Comparator.comparing(Producto::getCodigo))
                .map(ProductoResponse::desde)
                .toList();
    }

    private boolean tieneCapacidad(Producto producto, int personasMinimas) {
        if (producto.getTamanos() == null || producto.getTamanos().isEmpty()) {
            return false;
        }
        return producto.getTamanos().stream().anyMatch(t -> t.getPersonas() >= personasMinimas);
    }

    private Producto aplicarDatos(Producto producto, ProductoRequest datos, Categoria categoria) {
        producto.setNombre(datos.nombre().trim());
        producto.setDescripcion(datos.descripcion());
        producto.setCategoria(categoria);
        producto.setPrecio(datos.precio());
        producto.setStock(datos.stock());
        producto.setOferta(datos.oferta());
        producto.setDestacado(datos.destacado());
        producto.setActivo(datos.activo());
        producto.setForma(datos.forma());
        producto.setPermiteMensaje(datos.permiteMensaje());
        producto.setImagen(datos.imagen());
        if (producto.getTamanos() == null) {
            producto.setTamanos(new ArrayList<>());
        }
        List<TamanoDto> tamanos = datos.tamanos() == null ? List.of() : datos.tamanos();
        producto.getTamanos().clear();
        producto.getTamanos().addAll(tamanos.stream().map(TamanoDto::aEntidad).toList());
        return producto;
    }

    private Producto buscar(String codigo) {
        return productoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con código " + codigo));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + id));
    }
}