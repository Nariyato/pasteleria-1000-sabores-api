package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pasteleria.dto.UsuarioResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class UsuarioControllerTest {

    private static final String RUTA = "/api/v1/usuarios";
    private static final String JSON_VALIDO =
            "{\"run\":\"123456780\",\"nombre\":\"Nuevo\",\"apellidos\":\"Usuario\",\"email\":\"nuevo@test.cl\","
                    + "\"fechaNacimiento\":\"1990-01-01\",\"password\":\"1234\",\"rol\":\"cliente\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UsuarioResponse usuarioEjemplo() {
        return new UsuarioResponse(1L, "123456780", "Admin", "Duoc", "admin@duoc.cl", LocalDate.of(1990, 1, 1), "admin",
                null, LocalDate.now(), null, null, null, null, null, true, "duoc");
    }

    @Test
    void listarSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void listarComoClienteResponde403() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarComoAdminResponde200SinCampoPassword() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(usuarioEjemplo()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@duoc.cl"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerPorIdResponde404() throws Exception {
        when(usuarioService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        mockMvc.perform(get(RUTA + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearResponde201ConLocation() throws Exception {
        when(usuarioService.crear(any())).thenReturn(usuarioEjemplo());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/usuarios/1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConCorreoRepetidoResponde409() throws Exception {
        when(usuarioService.crear(any())).thenThrow(new ReglaNegocioException("El correo ya está registrado"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarResponde200() throws Exception {
        when(usuarioService.actualizar(eq(1L), any())).thenReturn(usuarioEjemplo());

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@duoc.cl", roles = "ADMIN")
    void eliminarResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/5").with(user("admin@duoc.cl").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminar(5L, "admin@duoc.cl");
    }

    @Test
    @WithMockUser(username = "admin@duoc.cl", roles = "ADMIN")
    void eliminarPropioUsuarioResponde409() throws Exception {
        doThrow(new ReglaNegocioException("No puedes eliminar tu propio usuario"))
                .when(usuarioService).eliminar(1L, "admin@duoc.cl");

        mockMvc.perform(delete(RUTA + "/1").with(user("admin@duoc.cl").roles("ADMIN")))
                .andExpect(status().isConflict());
    }
}