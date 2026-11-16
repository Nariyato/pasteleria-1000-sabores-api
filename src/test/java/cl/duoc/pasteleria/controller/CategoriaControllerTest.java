package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pasteleria.dto.CategoriaResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.CategoriaService;

/** Prueba solo la capa web: el service va simulado (mock), no se toca la base de datos. */
@WebMvcTest(CategoriaController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class CategoriaControllerTest {

    private static final String RUTA = "/api/v1/categorias";
    private static final String JSON_VALIDO = "{\"nombre\":\"Tortas\",\"descripcion\":\"Desc\",\"imagen\":\"/img/a.jpg\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @Test
    void listarResponde200ConLaLista() throws Exception {
        when(categoriaService.listar()).thenReturn(List.of(
                new CategoriaResponse(1L, "Tortas", "Desc", "/img/a.jpg"),
                new CategoriaResponse(2L, "Postres", null, null)));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Tortas"));
    }

    @Test
    void obtenerResponde404SiNoExiste() throws Exception {
        when(categoriaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe la categoría con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("No existe la categoría con id 99"))
                .andExpect(jsonPath("$.ruta").value(RUTA + "/99"));
    }

    @Test
    void crearResponde201ConLocation() throws Exception {
        when(categoriaService.crear(any())).thenReturn(new CategoriaResponse(10L, "Tortas", "Desc", "/img/a.jpg"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", RUTA + "/10"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void crearResponde400ConErroresPorCampoSiElNombreVieneVacio() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crearResponde400SiElJsonEstaMalFormado() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearResponde409SiElNombreYaExiste() throws Exception {
        when(categoriaService.crear(any())).thenThrow(new ReglaNegocioException("Ya existe una categoría llamada Tortas"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void actualizarResponde200() throws Exception {
        when(categoriaService.actualizar(eq(1L), any())).thenReturn(new CategoriaResponse(1L, "Tortas", "Desc", "/img/a.jpg"));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tortas"));
    }

    @Test
    void eliminarResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarResponde409SiTieneProductos() throws Exception {
        doThrow(new ReglaNegocioException("La categoría tiene productos asociados")).when(categoriaService).eliminar(1L);

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void idConTipoIncorrectoResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc"))
                .andExpect(status().isBadRequest());
    }
}
