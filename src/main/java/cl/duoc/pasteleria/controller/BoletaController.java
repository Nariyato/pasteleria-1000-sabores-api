package cl.duoc.pasteleria.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.BoletaRequest;
import cl.duoc.pasteleria.dto.BoletaResponse;
import cl.duoc.pasteleria.dto.EstadoRequest;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Compras, listados y estado de despacho")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @Operation(summary = "Crear una boleta (compra)")
    @ApiResponse(responseCode = "201", description = "Boleta creada")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Sin stock u otra regla de negocio",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<BoletaResponse> crear(@Valid @RequestBody BoletaRequest datos,
            @AuthenticationPrincipal UserDetails sesion) {
        BoletaResponse creada = boletaService.crear(sesion.getUsername(), datos);
        return ResponseEntity.created(URI.create("/api/v1/boletas/" + creada.id())).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar todas las boletas (admin y vendedor)")
    @ApiResponse(responseCode = "200", description = "Lista de boletas")
    public List<BoletaResponse> listar() {
        return boletaService.listar();
    }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Boletas del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista ordenada por fecha descendente")
    public List<BoletaResponse> misBoletas(@AuthenticationPrincipal UserDetails sesion) {
        return boletaService.listarMias(sesion.getUsername());
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Boletas de un usuario (solo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista de boletas del usuario")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<BoletaResponse> listarPorUsuario(@PathVariable Long usuarioId) {
        return boletaService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta")
    @ApiResponse(responseCode = "200", description = "Boleta encontrada")
    @ApiResponse(responseCode = "403", description = "Cliente consultando boleta ajena",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "Boleta no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public BoletaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UserDetails sesion) {
        return boletaService.obtener(id, sesion.getUsername());
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de despacho (solo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado no válido",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest datos) {
        return boletaService.cambiarEstado(id, datos);
    }
}