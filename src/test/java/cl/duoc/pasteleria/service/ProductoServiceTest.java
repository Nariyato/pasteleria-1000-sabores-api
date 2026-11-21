package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pasteleria.dto.ProductoRequest;
import cl.duoc.pasteleria.dto.ProductoResponse;
import cl.duoc.pasteleria.dto.TamanoDto;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Categoria;
import cl.duoc.pasteleria.model.Producto;
import cl.duoc.pasteleria.model.Tamano;
import cl.duoc.pasteleria.repository.CategoriaRepository;
import cl.duoc.pasteleria.repository.DetalleBoletaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private DetalleBoletaRepository detalleBoletaRepository;

    @InjectMocks
    private ProductoService productoService;

  private Categoria categoria(Long id) {
    return Categoria.builder().id(id).nombre("Tortas").build();
  }

    private Producto producto(String codigo, String nombre, Long categoriaId, boolean activo, int stock, int oferta,
            String forma, int precio, boolean destacado) {
        return Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .categoria(categoria(categoriaId))
                .activo(activo)
                .stock(stock)
                .oferta(oferta)
                .forma(forma)
                .precio(precio)
                .destacado(destacado)
                .permiteMensaje(false)
                .tamanos(new ArrayList<>(List.of(new Tamano("mediana", 15, 10000))))
                .build();
    }

    private ProductoRequest request(String codigo, Long categoriaId) {
        return new ProductoRequest(codigo, "Nuevo", "Desc", categoriaId, 12000, 8, 0, false, true, "cuadrada", true,
                "/img.jpg", List.of(new TamanoDto("chica", 8, 0)));
    }

    @Test
    void buscarSinFiltrosDevuelveActivosOrdenados() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(
                producto("TC002", "B", 1L, true, 10, 0, "cuadrada", 20000, false),
                producto("TC001", "A", 1L, true, 10, 0, "cuadrada", 15000, false)));

        List<ProductoResponse> resultado = productoService.buscar(null, null, null, null, null, null);

        assertThat(resultado).extracting(ProductoResponse::codigo).containsExactly("TC001", "TC002");
    }

    @Test
    void buscarPorTextoIgnoraMayusculas() {
        when(productoRepository.findByActivoTrue())
                .thenReturn(List.of(producto("TC001", "Chocolate", 1L, true, 10, 0, "cuadrada", 10000, false)));

        assertThat(productoService.buscar("choco", null, null, null, null, null)).hasSize(1);
        assertThat(productoService.buscar("vainilla", null, null, null, null, null)).isEmpty();
    }

    @Test
    void buscarPorCategoriaId() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(
                producto("TC001", "A", 1L, true, 10, 0, "cuadrada", 10000, false),
                producto("PI001", "B", 2L, true, 10, 0, null, 10000, false)));

        assertThat(productoService.buscar(null, 2L, null, null, null, null))
                .extracting(ProductoResponse::codigo).containsExactly("PI001");
    }

    @Test
    void buscarPorForma() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(
                producto("TC001", "A", 1L, true, 10, 0, "cuadrada", 10000, false),
                producto("TT001", "B", 1L, true, 10, 0, "circular", 10000, false)));

        assertThat(productoService.buscar(null, null, "circular", null, null, null))
                .extracting(ProductoResponse::codigo).containsExactly("TT001");
    }

    @Test
    void buscarPorPersonas() {
        Producto torta = producto("TC001", "A", 1L, true, 10, 0, "cuadrada", 10000, false);
        torta.getTamanos().add(new Tamano("grande", 25, 20000));
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(torta));

        assertThat(productoService.buscar(null, null, null, 20, null, null)).hasSize(1);
        assertThat(productoService.buscar(null, null, null, 30, null, null)).isEmpty();
    }

    @Test
    void buscarPorRangoDePrecio() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(
                producto("A", "A", 1L, true, 10, 0, null, 5000, false),
                producto("B", "B", 1L, true, 10, 0, null, 15000, false)));

        assertThat(productoService.buscar(null, null, null, null, 10000, 20000))
                .extracting(ProductoResponse::codigo).containsExactly("B");
    }

    @Test
    void buscarExcluyeInactivos() {
        when(productoRepository.findByActivoTrue()).thenReturn(
                List.of(producto("TC001", "A", 1L, true, 10, 0, null, 10000, false)));

        assertThat(productoService.buscar(null, null, null, null, null, null)).hasSize(1);
        verify(productoRepository, never()).findAll();
    }

    @Test
    void listarDestacadosSoloActivosDestacados() {
        when(productoRepository.findByActivoTrueAndDestacadoTrue())
                .thenReturn(List.of(producto("TC001", "A", 1L, true, 10, 0, null, 10000, true)));

        assertThat(productoService.listarDestacados()).hasSize(1);
    }

    @Test
    void listarOfertasUsaOrdenDelRepositorio() {
        Producto ofertaAlta = producto("A", "A", 1L, true, 10, 50, null, 10000, false);
        Producto ofertaBaja = producto("B", "B", 1L, true, 10, 10, null, 10000, false);
        when(productoRepository.findByActivoTrueAndOfertaGreaterThanOrderByOfertaDesc(0))
                .thenReturn(List.of(ofertaAlta, ofertaBaja));

        assertThat(productoService.listarOfertas())
                .extracting(ProductoResponse::oferta)
                .containsExactly(50, 10);
    }

    @Test
    void listarCriticosUsaStockMenorACinco() {
        when(productoRepository.findByStockLessThan(ProductoService.STOCK_CRITICO))
                .thenReturn(List.of(producto("A", "A", 1L, true, 4, 0, null, 10000, false)));

        assertThat(productoService.listarCriticos()).hasSize(1);
        verify(productoRepository).findByStockLessThan(5);
    }

    @Test
    void obtenerPorCodigoLanzaNoEncontrado() {
        when(productoRepository.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorCodigo("X"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearOk() {
        when(productoRepository.existsById("NX001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse creado = productoService.crear(request("NX001", 1L));

        assertThat(creado.codigo()).isEqualTo("NX001");
    }

    @Test
    void crearCodigoRepetidoLanzaReglaNegocio() {
        when(productoRepository.existsById("TC001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(request("TC001", 1L)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void crearCategoriaInexistenteLanzaNoEncontrado() {
        when(productoRepository.existsById("NX001")).thenReturn(false);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(request("NX001", 99L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarOkReemplazaTamanos() {
        Producto existente = producto("TC001", "Viejo", 1L, true, 10, 0, "cuadrada", 10000, false);
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoRequest datos = new ProductoRequest("OTR", "Nuevo nombre", "D", 1L, 11000, 9, 5, true, true,
                "cuadrada", true, "/n.jpg", List.of(new TamanoDto("grande", 20, 5000)));
        ProductoResponse actualizado = productoService.actualizar("TC001", datos);

        assertThat(actualizado.nombre()).isEqualTo("Nuevo nombre");
        assertThat(actualizado.tamanos()).hasSize(1);
        assertThat(actualizado.tamanos().get(0).id()).isEqualTo("grande");
    }

    @Test
    void actualizarProductoInexistenteLanzaNoEncontrado() {
        when(productoRepository.findById("ZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar("ZZZ", request("ZZZ", 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarOk() {
        Producto existente = producto("TC001", "A", 1L, true, 10, 0, null, 10000, false);
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(existente));
        when(detalleBoletaRepository.existsByProducto_Codigo("TC001")).thenReturn(false);

        productoService.eliminar("TC001");

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).delete(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("TC001");
    }

    @Test
    void eliminarConVentasLanzaReglaNegocio() {
        when(productoRepository.findById("TC001")).thenReturn(Optional.of(producto("TC001", "A", 1L, true, 10, 0, null, 10000, false)));
        when(detalleBoletaRepository.existsByProducto_Codigo("TC001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.eliminar("TC001")).isInstanceOf(ReglaNegocioException.class);
    }
}