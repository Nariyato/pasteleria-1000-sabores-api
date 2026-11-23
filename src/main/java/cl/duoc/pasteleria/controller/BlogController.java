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
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.BlogRequest;
import cl.duoc.pasteleria.dto.BlogResponse;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Leer el blog es público; crear, editar y eliminar entradas es solo del ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/blog")
@Tag(name = "Blog", description = "Entradas del blog de la pastelería")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    @Operation(summary = "Listar las entradas del blog (la más nueva primero)")
    @ApiResponse(responseCode = "200", description = "Lista de entradas")
    public List<BlogResponse> listar() {
        return blogService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una entrada del blog por su id")
    @ApiResponse(responseCode = "200", description = "Entrada encontrada")
    @ApiResponse(responseCode = "404", description = "La entrada no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public BlogResponse obtener(@PathVariable Long id) {
        return blogService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una entrada del blog")
    @ApiResponse(responseCode = "201", description = "Entrada creada")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "No hay sesión",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<BlogResponse> crear(@Valid @RequestBody BlogRequest datos) {
        BlogResponse creada = blogService.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/blog/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una entrada del blog")
    @ApiResponse(responseCode = "200", description = "Entrada actualizada")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "No hay sesión",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "La entrada no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public BlogResponse actualizar(@PathVariable Long id, @Valid @RequestBody BlogRequest datos) {
        return blogService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una entrada del blog")
    @ApiResponse(responseCode = "204", description = "Entrada eliminada")
    @ApiResponse(responseCode = "401", description = "No hay sesión",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "La entrada no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        blogService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
