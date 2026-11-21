package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.duoc.pasteleria.dto.LoginRequest;
import cl.duoc.pasteleria.dto.PerfilRequest;
import cl.duoc.pasteleria.dto.RegistroRequest;
import cl.duoc.pasteleria.dto.RefreshRequest;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.UsuarioRepository;
import cl.duoc.pasteleria.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Clock RELOJ = Clock.fixed(
      Instant.parse("2026-10-22T15:00:00Z"),
      ZoneId.of("America/Santiago"));

  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private UsuarioRepository usuarioRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void prepararJwt() {
    authService = new AuthService(authenticationManager, usuarioRepository, passwordEncoder, jwtService, RELOJ);
  }

  @Test
  void loginOk() {
    Usuario usuario = usuarioCliente();
    when(usuarioRepository.findByEmailIgnoreCase("rosa@gmail.com")).thenReturn(java.util.Optional.of(usuario));
    when(jwtService.generarAccessToken(usuario)).thenReturn("access");
    when(jwtService.generarRefreshToken(usuario)).thenReturn("refresh");
    when(jwtService.segundosAccess()).thenReturn(1800L);

    var respuesta = authService.login(new LoginRequest("rosa@gmail.com", "1234"));

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    assertThat(respuesta.accessToken()).isEqualTo("access");
    assertThat(respuesta.usuario().email()).isEqualTo("rosa@gmail.com");
  }

  @Test
  void registroOk() {
    var datos = new RegistroRequest("123456780", "Ana", "López", "ana@gmail.com",
        LocalDate.of(1995, 1, 1), "1234", "felices50");
    when(usuarioRepository.existsByEmailIgnoreCase("ana@gmail.com")).thenReturn(false);
    when(passwordEncoder.encode("1234")).thenReturn("$2a$10$abcdefghijklmnopqrstuvwx1234567890abcd");
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
      Usuario u = inv.getArgument(0);
      u.setId(99L);
      return u;
    });
    when(jwtService.generarAccessToken(any())).thenReturn("a");
    when(jwtService.generarRefreshToken(any())).thenReturn("r");
    when(jwtService.segundosAccess()).thenReturn(1800L);

    var respuesta = authService.registrar(datos);

    assertThat(respuesta.usuario().rol()).isEqualTo("cliente");
    assertThat(respuesta.usuario().codigoDescuento()).isEqualTo("FELICES50");
    assertThat(respuesta.usuario().fechaRegistro()).isEqualTo(LocalDate.of(2026, 10, 22));
    verify(passwordEncoder).encode("1234");
  }

  @Test
  void registroCorreoRepetido() {
    when(usuarioRepository.existsByEmailIgnoreCase("rosa@gmail.com")).thenReturn(true);
    assertThatThrownBy(() -> authService.registrar(registroBasico("rosa@gmail.com")))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessage("El correo ya está registrado");
  }

  @Test
  void registroCodigoInvalido() {
    when(usuarioRepository.existsByEmailIgnoreCase("nuevo@gmail.com")).thenReturn(false);
    var datos = new RegistroRequest("123456780", "Nombre", "Apellido", "nuevo@gmail.com",
        LocalDate.of(1990, 1, 1), "1234", "INVALIDO");
    assertThatThrownBy(() -> authService.registrar(datos))
        .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void refreshConAccessTokenLanzaError() {
    when(jwtService.emailSiValido("token-access", JwtService.TIPO_REFRESH)).thenReturn(null);
    assertThatThrownBy(() -> authService.renovar(new RefreshRequest("token-access")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("El token de renovación es inválido o expiró");
  }

  @Test
  void actualizarPerfil() {
    Usuario usuario = usuarioCliente();
    when(usuarioRepository.findByEmailIgnoreCase("rosa@gmail.com")).thenReturn(java.util.Optional.of(usuario));

    var perfil = authService.actualizarPerfil("rosa@gmail.com",
        new PerfilRequest("Rosa Nueva", "Muñoz", "Calle 1", "Santiago", "RM", "+56912345678", null));

    assertThat(perfil.nombre()).isEqualTo("Rosa Nueva");
    assertThat(perfil.foto()).isNull();
  }

  private static RegistroRequest registroBasico(String email) {
    return new RegistroRequest("123456780", "Nombre", "Apellido", email, LocalDate.of(1990, 1, 1), "1234", null);
  }

  private static Usuario usuarioCliente() {
    return Usuario.builder()
        .id(1L)
        .email("rosa@gmail.com")
        .nombre("Rosa")
        .rol(Rol.CLIENTE)
        .fechaRegistro(LocalDate.of(2026, 10, 22))
        .build();
  }
}