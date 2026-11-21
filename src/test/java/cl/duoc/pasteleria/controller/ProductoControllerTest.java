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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pasteleria.dto.ProductoResponse;
import cl.duoc.pasteleria.dto.TamanoDto;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.ProductoService;

@WebMvcTest(ProductoController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class ProductoControllerTest {

    private static final String RUTA = "/api/v1/productos";
    private static final String JSON_VALIDO =
            "{\"codigo\":\"NX001\",\"nombre\":\"Nuevo\",\"categoriaId\":1,\"precio\":12000,\"stock\":5,"
                    + "\"oferta\":0,\"destacado\":false,\"activo\":true,\"permiteMensaje\":false,"
                    + "\"tamanos\":[{\"id\":\"chica\",\"personas\":8,\"extra\":0}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ProductoResponse productoEjemplo() {
        return new ProductoResponse("TC001", "Torta", "Desc", 1L, 12000, 5, 0, false, true, "cuadrada", true,
                "/img.jpg", List.of(new TamanoDto("chica", 8, 0)));
    }

    @Test
    void buscarEsPublicoSinToken() throws Exception {
        when(productoService.buscar(null, null, null, null, null, null)).thenReturn(List.of(productoEjemplo()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarTodosConAdminResponde200() throws Exception {
        when(productoService.listarTodos()).thenReturn(List.of(productoEjemplo()));

        mockMvc.perform(get(RUTA + "/todos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void listarTodosConClienteResponde403() throws Exception {
        mockMvc.perform(get(RUTA + "/todos")).andExpect(status().isForbidden());
    }

    @Test
    void listarTodosSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RUTA + "/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void criticosConAdminResponde200() throws Exception {
        when(productoService.listarCriticos()).thenReturn(List.of());

        mockMvc.perform(get(RUTA + "/criticos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void criticosConClienteResponde403() throws Exception {
        mockMvc.perform(get(RUTA + "/criticos")).andExpect(status().isForbidden());
    }

    @Test
    void criticosSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RUTA + "/criticos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearResponde201ConLocation() throws Exception {
        when(productoService.crear(any())).thenReturn(new ProductoResponse("NX001", "Nuevo", null, 1L, 12000, 5, 0,
                false, true, null, false, null, List.of()));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/productos/NX001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConPrecioInvalidoResponde400() throws Exception {
        String json = JSON_VALIDO.replace("\"precio\":12000", "\"precio\":0");

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void crearConVendedorResponde403() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crearConClienteResponde403() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarResponde404SiNoExiste() throws Exception {
        when(productoService.actualizar(eq("ZZZ"), any()))
                .thenThrow(new RecursoNoEncontradoException("No existe el producto con código ZZZ"));

        mockMvc.perform(put(RUTA + "/ZZZ").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/TC001")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarConVentasResponde409() throws Exception {
        doThrow(new ReglaNegocioException("tiene ventas")).when(productoService).eliminar("TC001");

        mockMvc.perform(delete(RUTA + "/TC001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }
}