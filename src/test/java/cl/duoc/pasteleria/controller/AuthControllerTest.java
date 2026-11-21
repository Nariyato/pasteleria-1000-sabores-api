package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pasteleria.dto.AuthResponse;
import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class AuthControllerTest {

    private static final String RUTA = "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

  private static final UsuarioResponse USUARIO_ADMIN = new UsuarioResponse(
      1L, "111111111", "Admin", "Mil Sabores", "admin@duoc.cl",
      LocalDate.of(1990, 1, 1), "admin", null, LocalDate.of(2026, 10, 22),
      null, null, null, null, null, false, "normal");

    private static final AuthResponse AUTH_OK = new AuthResponse("access", "refresh", "Bearer", 1800, USUARIO_ADMIN);

    @Test
    void loginOk() throws Exception {
        when(authService.login(any())).thenReturn(AUTH_OK);

        mockMvc.perform(post(RUTA + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@duoc.cl\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void loginClaveMala() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(RUTA + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@duoc.cl\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void registroCreado() throws Exception {
        when(authService.registrar(any())).thenReturn(AUTH_OK);

        mockMvc.perform(post(RUTA + "/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"run":"123456780","nombre":"Ana","apellidos":"López","email":"ana@gmail.com",\
                                "fechaNacimiento":"1995-01-01","password":"1234"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void registroCorreoRepetido() throws Exception {
        when(authService.registrar(any())).thenThrow(new ReglaNegocioException("El correo ya está registrado"));

        mockMvc.perform(post(RUTA + "/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"run":"123456780","nombre":"Ana","apellidos":"López","email":"rosa@gmail.com",\
                                "fechaNacimiento":"1995-01-01","password":"1234"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void refreshInvalido() throws Exception {
        when(authService.renovar(any())).thenThrow(
                new BadCredentialsException("El token de renovación es inválido o expiró"));

        mockMvc.perform(post(RUTA + "/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"basura\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El token de renovación es inválido o expiró"));
    }

    @Test
    void meSinToken() throws Exception {
        mockMvc.perform(get(RUTA + "/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meConUsuario() throws Exception {
        when(authService.perfil("admin@duoc.cl")).thenReturn(USUARIO_ADMIN);

        mockMvc.perform(get(RUTA + "/me").with(user("admin@duoc.cl").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@duoc.cl"));
    }

    @Test
    void actualizarPerfil() throws Exception {
        when(authService.actualizarPerfil(any(), any())).thenReturn(USUARIO_ADMIN);

        mockMvc.perform(put(RUTA + "/perfil").with(user("admin@duoc.cl").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Admin\",\"apellidos\":\"Mil Sabores\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginSinEmail() throws Exception {
        mockMvc.perform(post(RUTA + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"1234\"}"))
                .andExpect(status().isBadRequest());
    }
}