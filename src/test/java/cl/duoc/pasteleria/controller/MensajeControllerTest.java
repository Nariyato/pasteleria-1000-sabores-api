package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import cl.duoc.pasteleria.dto.MensajeResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.MensajeService;

/** Capa web de mensajes con el service simulado: códigos HTTP y reglas de rol de SecurityConfig. */
@WebMvcTest(MensajeController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class MensajeControllerTest {

    private static final String RUTA = "/api/v1/mensajes";
    private static final String JSON_VALIDO =
            "{\"tipo\":\"Reclamo\",\"nombre\":\"Rosa\",\"email\":\"rosa@gmail.com\",\"mensaje\":\"Llegó tarde\"}";
    private static final MensajeResponse RESPUESTA = new MensajeResponse(7L, "Reclamo", "Rosa", "rosa@gmail.com",
            "Llegó tarde", null, Instant.parse("2026-11-18T15:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeService mensajeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void crearSinSesionResponde201ConLocationYSinUsuario() throws Exception {
        when(mensajeService.crear(any(), isNull())).thenReturn(RESPUESTA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", RUTA + "/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    @WithMockUser(username = "rosa@gmail.com", roles = "CLIENTE")
    void crearConSesionPasaElCorreoDelTokenAlService() throws Exception {
        when(mensajeService.crear(any(), eq("rosa@gmail.com"))).thenReturn(RESPUESTA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated());

        verify(mensajeService).crear(any(), eq("rosa@gmail.com"));
    }

    @Test
    void crearConCamposVaciosResponde400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"\",\"nombre\":\"\",\"email\":\"no-es-correo\",\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo").value("El tipo de consulta es obligatorio"))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errores.email").value("El correo no es válido"))
                .andExpect(jsonPath("$.errores.mensaje").value("El mensaje es obligatorio"));
        verify(mensajeService, never()).crear(any(), any());
    }

    @Test
    void crearConMensajeDeMasDe500CaracteresResponde400() throws Exception {
        String largo = "a".repeat(501);
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"tipo\":\"Reclamo\",\"nombre\":\"Rosa\",\"email\":\"rosa@gmail.com\",\"mensaje\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.mensaje").value("El mensaje no puede superar los 500 caracteres"));
    }

    @Test
    void listarSinSesionResponde401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void listarComoClienteResponde403() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void listarComoVendedorResponde403() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarComoAdminResponde200() throws Exception {
        when(mensajeService.listar()).thenReturn(List.of(RESPUESTA));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("Reclamo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerResponde404SiNoExiste() throws Exception {
        when(mensajeService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe el mensaje con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el mensaje con id 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarComoAdminResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/7")).andExpect(status().isNoContent());
        verify(mensajeService).eliminar(7L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarResponde404SiNoExiste() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el mensaje con id 99")).when(mensajeService).eliminar(99L);

        mockMvc.perform(delete(RUTA + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void eliminarComoClienteResponde403() throws Exception {
        mockMvc.perform(delete(RUTA + "/7")).andExpect(status().isForbidden());
        verify(mensajeService, never()).eliminar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editarNoExisteResponde405() throws Exception {
        mockMvc.perform(put(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isMethodNotAllowed());
    }
}
