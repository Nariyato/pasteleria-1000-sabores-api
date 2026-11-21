package cl.duoc.pasteleria.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.AuthResponse;
import cl.duoc.pasteleria.dto.LoginRequest;
import cl.duoc.pasteleria.dto.PerfilRequest;
import cl.duoc.pasteleria.dto.RefreshRequest;
import cl.duoc.pasteleria.dto.RegistroRequest;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.UsuarioRepository;
import cl.duoc.pasteleria.security.JwtService;

@Service
public class AuthService {

    private static final String CODIGO_FELICES = "FELICES50";
    private static final String MSG_REFRESH_INVALIDO = "El token de renovación es inválido o expiró";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock reloj;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, Clock reloj) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.reloj = reloj;
    }

    public AuthResponse login(LoginRequest datos) {
        String email = datos.email().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, datos.password()));
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario"));
        return tokensPara(usuario);
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest datos) {
        String email = datos.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        String codigoGuardado = null;
        String codigo = datos.codigoDescuento();
        if (codigo != null && !codigo.isBlank()) {
            if (!CODIGO_FELICES.equalsIgnoreCase(codigo.trim())) {
                throw new ReglaNegocioException("El código de descuento no es válido");
            }
            codigoGuardado = CODIGO_FELICES;
        }

        Usuario usuario = Usuario.builder()
                .run(datos.run().trim())
                .nombre(datos.nombre().trim())
                .apellidos(datos.apellidos().trim())
                .email(email)
                .password(passwordEncoder.encode(datos.password()))
                .fechaNacimiento(datos.fechaNacimiento())
                .rol(Rol.CLIENTE)
                .codigoDescuento(codigoGuardado)
                .fechaRegistro(LocalDate.now(reloj))
                .build();

        usuario = usuarioRepository.save(usuario);
        return tokensPara(usuario);
    }

    public AuthResponse renovar(RefreshRequest datos) {
        String email = jwtService.emailSiValido(datos.refreshToken(), JwtService.TIPO_REFRESH);
        if (email == null) {
            throw new BadCredentialsException(MSG_REFRESH_INVALIDO);
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(MSG_REFRESH_INVALIDO));
        return tokensPara(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario"));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(String email, PerfilRequest datos) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario"));

        usuario.setNombre(datos.nombre().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setDireccion(datos.direccion());
        usuario.setComuna(datos.comuna());
        usuario.setRegion(datos.region());
        if (datos.telefono() != null && !datos.telefono().isBlank()) {
            usuario.setTelefono(datos.telefono().trim());
        } else {
            usuario.setTelefono(null);
        }
        usuario.setFoto(datos.foto());

        return UsuarioResponse.desde(usuario);
    }

    private AuthResponse tokensPara(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                "Bearer",
                jwtService.segundosAccess(),
                UsuarioResponse.desde(usuario));
    }
}