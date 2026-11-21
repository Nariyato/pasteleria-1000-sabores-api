package cl.duoc.pasteleria.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.UsuarioRequest;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Administración de usuarios (solo ADMIN)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios sin contraseña")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    @PostMapping
    @Operation(summary = "Crear un usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo repetido o código inválido",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest datos) {
        UsuarioResponse creado = usuarioService.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo de otro usuario o conflicto de negocio",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos) {
        return usuarioService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Tiene boletas o es el usuario autenticado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UserDetails sesion) {
        String email = sesion != null ? sesion.getUsername() : null;
        usuarioService.eliminar(id, email);
        return ResponseEntity.noContent().build();
    }
}