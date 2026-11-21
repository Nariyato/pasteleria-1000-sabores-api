package cl.duoc.pasteleria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pasteleria.dto.AuthResponse;
import cl.duoc.pasteleria.dto.LoginRequest;
import cl.duoc.pasteleria.dto.PerfilRequest;
import cl.duoc.pasteleria.dto.RefreshRequest;
import cl.duoc.pasteleria.dto.RegistroRequest;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.ErrorRespuesta;
import cl.duoc.pasteleria.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, registro, renovación de tokens y perfil")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión con correo y contraseña")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest datos) {
        return authService.login(datos);
    }

    @PostMapping("/registro")
    @SecurityRequirements
    @Operation(summary = "Registrar un nuevo cliente")
    @ApiResponse(responseCode = "201", description = "Usuario registrado e iniciada la sesión")
    @ApiResponse(responseCode = "409", description = "Correo repetido o código de descuento inválido",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest datos) {
        AuthResponse respuesta = authService.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar access token con el refresh token")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Token de renovación inválido o expirado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest datos) {
        return authService.renovar(datos);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario")
    @ApiResponse(responseCode = "401", description = "Sin sesión",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse me(@AuthenticationPrincipal UserDetails usuario) {
        return authService.perfil(usuario.getUsername());
    }

    @PutMapping("/perfil")
    @Operation(summary = "Actualizar datos del perfil")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody PerfilRequest datos) {
        return authService.actualizarPerfil(usuario.getUsername(), datos);
    }
}