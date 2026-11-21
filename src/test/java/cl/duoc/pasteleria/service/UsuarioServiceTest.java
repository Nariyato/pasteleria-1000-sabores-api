package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.duoc.pasteleria.dto.UsuarioRequest;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.exception.ValidacionCamposException;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Santiago"));

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BoletaRepository boletaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurarReloj() {
        usuarioService = new UsuarioService(usuarioRepository, boletaRepository, passwordEncoder, RELOJ);
    }

    private Usuario usuario(Long id, String email, Rol rol) {
        return Usuario.builder()
                .id(id)
                .run("123456780")
                .nombre("Ana")
                .apellidos("Pérez")
                .email(email)
                .password("hash-bcrypt-de-sesenta-caracteres-x")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .rol(rol)
                .fechaRegistro(LocalDate.of(2025, 1, 1))
                .build();
    }

    private UsuarioRequest requestCompleto(String email, String password, String rol) {
        return new UsuarioRequest("123456780", "Ana", "Pérez", email, LocalDate.of(1990, 1, 1), password, null, rol,
                null, null, null, null, null);
    }

    @Test
    void listarOrdenaPorId() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario(2L, "b@test.cl", Rol.CLIENTE),
                usuario(1L, "a@test.cl", Rol.ADMIN)));

        List<UsuarioResponse> lista = usuarioService.listar();

        assertThat(lista).extracting(UsuarioResponse::email).containsExactly("a@test.cl", "b@test.cl");
    }

    @Test
    void obtenerPorIdNoEncontradoLanza404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearSinPasswordLanza400() {
        var datos = requestCompleto("nuevo@test.cl", null, "cliente");

        assertThatThrownBy(() -> usuarioService.crear(datos))
                .isInstanceOf(ValidacionCamposException.class);
    }

    @Test
    void crearConCorreoRepetidoLanza409() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(requestCompleto("nuevo@test.cl", "1234", "cliente")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void crearGuardaCorreoEnMinusculasYPasswordHasheada() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@test.cl")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        usuarioService.crear(requestCompleto(" Nuevo@TEST.cl ", "1234", "vendedor"));

        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("nuevo@test.cl");
        assertThat(cap.getValue().getPassword()).isEqualTo("encriptado");
        assertThat(cap.getValue().getRol()).isEqualTo(Rol.VENDEDOR);
        assertThat(cap.getValue().getFechaRegistro()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void actualizarSinPasswordNoLaCambia() {
        Usuario existente = usuario(1L, "ana@test.cl", Rol.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("ana@test.cl", 1L)).thenReturn(false);
        when(usuarioRepository.save(existente)).thenReturn(existente);

        String hashAnterior = existente.getPassword();
        usuarioService.actualizar(1L, requestCompleto("ana@test.cl", "", "cliente"));

        verify(passwordEncoder, never()).encode(any());
        assertThat(existente.getPassword()).isEqualTo(hashAnterior);
    }

    @Test
    void actualizarCorreoDeOtroUsuarioLanza409() {
        Usuario existente = usuario(1L, "ana@test.cl", Rol.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("rosa@gmail.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(1L, requestCompleto("rosa@gmail.com", null, "cliente")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void eliminarUsuarioConBoletasLanza409() {
        Usuario existente = usuario(5L, "ana@test.cl", Rol.CLIENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(boletaRepository.existsByUsuarioId(5L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.eliminar(5L, "admin@duoc.cl"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminarUsuarioAutenticadoLanza409() {
        Usuario existente = usuario(1L, "admin@duoc.cl", Rol.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.eliminar(1L, "admin@duoc.cl"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void eliminarUsuarioSinBoletasLoBorra() {
        Usuario existente = usuario(9L, "borrar@test.cl", Rol.CLIENTE);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(existente));
        when(boletaRepository.existsByUsuarioId(9L)).thenReturn(false);

        usuarioService.eliminar(9L, "admin@duoc.cl");

        verify(usuarioRepository).delete(existente);
    }
}