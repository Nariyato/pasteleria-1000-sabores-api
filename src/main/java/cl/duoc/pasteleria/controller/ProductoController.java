package cl.duoc.pasteleria.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.ProductoRequest;
import cl.duoc.pasteleria.dto.ProductoResponse;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo, búsqueda y administración de productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Buscar productos activos con filtros opcionales")
    @ApiResponse(responseCode = "200", description = "Lista de productos activos")
    public List<ProductoResponse> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String forma,
            @RequestParam(required = false) Integer personas,
            @RequestParam(required = false) Integer precioMin,
            @RequestParam(required = false) Integer precioMax) {
        return productoService.buscar(texto, categoriaId, forma, personas, precioMin, precioMax);
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los productos (incluye inactivos)")
    @ApiResponse(responseCode = "200", description = "Lista completa")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<ProductoResponse> listarTodos() {
        return productoService.listarTodos();
    }

    @GetMapping("/destacados")
    @Operation(summary = "Productos activos destacados")
    public List<ProductoResponse> destacados() {
        return productoService.listarDestacados();
    }

    @GetMapping("/ofertas")
    @Operation(summary = "Productos activos en oferta")
    public List<ProductoResponse> ofertas() {
        return productoService.listarOfertas();
    }

    @GetMapping("/criticos")
    @Operation(summary = "Productos con stock crítico")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<ProductoResponse> criticos() {
        return productoService.listarCriticos();
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtener un producto por código")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ProductoResponse obtener(@PathVariable String codigo) {
        return productoService.obtenerPorCodigo(codigo);
    }

    @PostMapping
    @Operation(summary = "Crear un producto")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "Categoría inexistente",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Código repetido",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest datos) {
        ProductoResponse creado = productoService.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.codigo())).body(creado);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Actualizar un producto")
    @ApiResponse(responseCode = "404", description = "Producto o categoría no encontrados",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ProductoResponse actualizar(@PathVariable String codigo, @Valid @RequestBody ProductoRequest datos) {
        return productoService.actualizar(codigo, datos);
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Eliminar un producto")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "409", description = "El producto tiene ventas",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        productoService.eliminar(codigo);
        return ResponseEntity.noContent().build();
    }
}