package cl.duoc.pasteleria.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.UsuarioRequest;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.exception.ValidacionCamposException;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String CODIGO_FELICES = "FELICES50";

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarioRepository, BoletaRepository boletaRepository,
            PasswordEncoder passwordEncoder, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.boletaRepository = boletaRepository;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(UsuarioResponse::desde)
                .toList();
    }

    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest datos) {
        validarPasswordEnCrear(datos.password());
        String email = normalizarCorreo(datos.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .run(datos.run().trim())
                .nombre(datos.nombre().trim())
                .apellidos(datos.apellidos().trim())
                .email(email)
                .password(passwordEncoder.encode(datos.password()))
                .fechaNacimiento(datos.fechaNacimiento())
                .rol(Rol.desdeJson(datos.rol()))
                .codigoDescuento(codigoDescuentoValido(datos.codigoDescuento()))
                .fechaRegistro(LocalDate.now(reloj))
                .build();
        aplicarDatosOpcionales(usuario, datos);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest datos) {
        Usuario usuario = buscar(id);
        String email = normalizarCorreo(datos.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ReglaNegocioException("Ya existe otro usuario con el correo " + email);
        }

        usuario.setRun(datos.run().trim());
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setEmail(email);
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setRol(Rol.desdeJson(datos.rol()));
        usuario.setCodigoDescuento(codigoDescuentoValido(datos.codigoDescuento()));

        if (datos.password() != null && !datos.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(datos.password()));
        }

        aplicarDatosOpcionales(usuario, datos);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, String emailActual) {
        Usuario usuario = buscar(id);
        if (emailActual != null && emailActual.equalsIgnoreCase(usuario.getEmail())) {
            throw new ReglaNegocioException("No puedes eliminar tu propio usuario");
        }
        if (boletaRepository.existsByUsuarioId(id)) {
            throw new ReglaNegocioException("No se puede eliminar el usuario porque tiene boletas asociadas");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }

    private static String normalizarCorreo(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase();
    }

    private static void validarPasswordEnCrear(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidacionCamposException(Map.of("password", "La contraseña es obligatoria"));
        }
    }

    private String codigoDescuentoValido(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        if (!CODIGO_FELICES.equalsIgnoreCase(codigo.trim())) {
            throw new ReglaNegocioException("El código de descuento no es válido");
        }
        return CODIGO_FELICES;
    }

    private static void aplicarDatosOpcionales(Usuario usuario, UsuarioRequest datos) {
        usuario.setDireccion(blancoANull(datos.direccion()));
        usuario.setComuna(blancoANull(datos.comuna()));
        usuario.setRegion(blancoANull(datos.region()));
        if (datos.telefono() != null && !datos.telefono().isBlank()) {
            usuario.setTelefono(datos.telefono().trim());
        } else {
            usuario.setTelefono(null);
        }
        usuario.setFoto(datos.foto());
    }

    private static String blancoANull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}