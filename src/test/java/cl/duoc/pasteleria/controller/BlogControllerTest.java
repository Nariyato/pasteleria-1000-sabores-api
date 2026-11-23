package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import cl.duoc.pasteleria.dto.BlogResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.BlogService;

/** Capa web del blog con el service simulado: códigos HTTP y reglas de rol de SecurityConfig. */
@WebMvcTest(BlogController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class BlogControllerTest {

    private static final String RUTA = "/api/v1/blog";
    private static final String JSON_VALIDO = "{\"titulo\":\"Merengue perfecto\",\"categoria\":\"Consejos de maestros\","
            + "\"fecha\":\"2026-11-18\",\"autor\":\"Freddy Camposo\",\"imagen\":\"/img/amasando.jpg\","
            + "\"resumen\":\"Resumen corto\",\"contenido\":[\"Primer párrafo\",\"Segundo párrafo\"]}";
    private static final BlogResponse RESPUESTA = new BlogResponse(7L, "Merengue perfecto", "Consejos de maestros",
            LocalDate.parse("2026-11-18"), "Freddy Camposo", "/img/amasando.jpg", "Resumen corto",
            List.of("Primer párrafo", "Segundo párrafo"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlogService blogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listarSinSesionResponde200() throws Exception {
        when(blogService.listar()).thenReturn(List.of(RESPUESTA));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Merengue perfecto"))
                .andExpect(jsonPath("$[0].contenido.length()").value(2));
    }

    @Test
    void obtenerSinSesionResponde200ConLaFechaEnFormatoIso() throws Exception {
        when(blogService.obtener(7L)).thenReturn(RESPUESTA);

        mockMvc.perform(get(RUTA + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.fecha").value("2026-11-18"));
    }

    @Test
    void obtenerResponde404SiNoExiste() throws Exception {
        when(blogService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe la entrada del blog con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la entrada del blog con id 99"));
    }

    @Test
    void crearSinSesionResponde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
        verify(blogService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crearComoClienteResponde403() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
        verify(blogService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearComoAdminResponde201ConLocation() throws Exception {
        when(blogService.crear(any())).thenReturn(RESPUESTA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", RUTA + "/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.titulo").value("Merengue perfecto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConCamposVaciosResponde400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"categoria\":\"\",\"contenido\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").value("El título es obligatorio"))
                .andExpect(jsonPath("$.errores.categoria").value("La categoría es obligatoria"))
                .andExpect(jsonPath("$.errores.fecha").value("La fecha es obligatoria"))
                .andExpect(jsonPath("$.errores.contenido").value("El contenido debe tener al menos un párrafo"));
        verify(blogService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConTituloDeMasDe200CaracteresResponde400() throws Exception {
        String largo = "a".repeat(201);
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"titulo\":\"" + largo + "\",\"categoria\":\"Recetas\",\"fecha\":\"2026-11-18\","
                                + "\"contenido\":[\"Un párrafo\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").value("El título no puede superar los 200 caracteres"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarComoAdminResponde200() throws Exception {
        when(blogService.actualizar(eq(7L), any())).thenReturn(RESPUESTA);

        mockMvc.perform(put(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Merengue perfecto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarResponde404SiNoExiste() throws Exception {
        when(blogService.actualizar(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("No existe la entrada del blog con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarSinSesionResponde401() throws Exception {
        mockMvc.perform(put(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void actualizarComoVendedorResponde403() throws Exception {
        mockMvc.perform(put(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());
        verify(blogService, never()).actualizar(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarComoAdminResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/7")).andExpect(status().isNoContent());
        verify(blogService).eliminar(7L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarResponde404SiNoExiste() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe la entrada del blog con id 99")).when(blogService).eliminar(99L);

        mockMvc.perform(delete(RUTA + "/99")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarSinSesionResponde401() throws Exception {
        mockMvc.perform(delete(RUTA + "/7")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void eliminarComoClienteResponde403() throws Exception {
        mockMvc.perform(delete(RUTA + "/7")).andExpect(status().isForbidden());
        verify(blogService, never()).eliminar(any());
    }
}
