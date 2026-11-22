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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.MensajeRequest;
import cl.duoc.pasteleria.dto.MensajeResponse;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.MensajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** El POST es público (formulario de contacto); leer y borrar la bandeja es solo del ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/mensajes")
@Tag(name = "Mensajes de contacto", description = "Formulario de contacto y bandeja del administrador")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @PostMapping
    @Operation(summary = "Enviar un mensaje de contacto",
            description = "Público. Si viene un token válido, el mensaje queda asociado a ese usuario.")
    @ApiResponse(responseCode = "201", description = "Mensaje recibido")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<MensajeResponse> crear(@Valid @RequestBody MensajeRequest datos,
            @AuthenticationPrincipal UserDetails sesion) {
        MensajeResponse creado = mensajeService.crear(datos, sesion == null ? null : sesion.getUsername());
        return ResponseEntity.created(URI.create("/api/v1/mensajes/" + creado.id())).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar los mensajes (el más nuevo primero)")
    @ApiResponse(responseCode = "200", description = "Lista de mensajes")
    @ApiResponse(responseCode = "401", description = "No hay sesión",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<MensajeResponse> listar() {
        return mensajeService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un mensaje por su id")
    @ApiResponse(responseCode = "200", description = "Mensaje encontrado")
    @ApiResponse(responseCode = "404", description = "El mensaje no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public MensajeResponse obtener(@PathVariable Long id) {
        return mensajeService.obtener(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un mensaje")
    @ApiResponse(responseCode = "204", description = "Mensaje eliminado")
    @ApiResponse(responseCode = "404", description = "El mensaje no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mensajeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
