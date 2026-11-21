package cl.duoc.pasteleria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import cl.duoc.pasteleria.dto.BoletaResponse;
import cl.duoc.pasteleria.exception.GlobalExceptionHandler;
import cl.duoc.pasteleria.security.JwtFilter;
import cl.duoc.pasteleria.security.JwtService;
import cl.duoc.pasteleria.security.RespuestaAccesoDenegado;
import cl.duoc.pasteleria.security.RespuestaNoAutenticado;
import cl.duoc.pasteleria.security.SecurityConfig;
import cl.duoc.pasteleria.service.BoletaService;

@WebMvcTest(BoletaController.class)
@Import({ SecurityConfig.class, JwtFilter.class, RespuestaNoAutenticado.class, RespuestaAccesoDenegado.class,
        GlobalExceptionHandler.class })
class BoletaControllerTest {

    private static final String RUTA = "/api/v1/boletas";
    private static final String JSON_COMPRA =
            "{\"items\":[{\"codigo\":\"PT001\",\"cantidad\":1}],"
                    + "\"despacho\":{\"nombre\":\"Ana\",\"calle\":\"Calle 1\",\"comuna\":\"Maipú\",\"fechaEntrega\":\"2026-11-20\"}}";

    private static final BoletaResponse RESPUESTA = new BoletaResponse(
            1L,
            "B-000001",
            1L,
            new BoletaResponse.Cliente("Ana Pérez", "rosa@gmail.com"),
            "preparacion",
            Instant.parse("2026-11-16T12:00:00Z"),
            List.of(),
            new BoletaResponse.Despacho("Ana", "Calle 1", "Maipú", LocalDate.of(2026, 11, 20)),
            3000,
            0,
            List.of(),
            3500,
            6500);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoletaService boletaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "rosa@gmail.com", roles = "CLIENTE")
    void crearCliente201() throws Exception {
        when(boletaService.crear(eq("rosa@gmail.com"), any())).thenReturn(RESPUESTA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_COMPRA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", RUTA + "/1"))
                .andExpect(jsonPath("$.numero").value("B-000001"));
    }

    @Test
    @WithMockUser(username = "admin@duoc.cl", roles = "ADMIN")
    void crearAdmin201() throws Exception {
        when(boletaService.crear(eq("admin@duoc.cl"), any())).thenReturn(RESPUESTA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_COMPRA))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "vendedor@pasteleria.cl", roles = "VENDEDOR")
    void crearVendedor403() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON_COMPRA))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rosa@gmail.com", roles = "CLIENTE")
    void crearItemsVacios400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"items\":[],\"despacho\":{\"nombre\":\"Ana\",\"calle\":\"Calle 1\",\"comuna\":\"Maipú\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "vendedor@pasteleria.cl", roles = "VENDEDOR")
    void listarVendedor200() throws Exception {
        when(boletaService.listar()).thenReturn(List.of(RESPUESTA));

        mockMvc.perform(get(RUTA)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "rosa@gmail.com", roles = "CLIENTE")
    void listarCliente403() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendedor@pasteleria.cl", roles = "VENDEDOR")
    void cambiarEstadoVendedor403() throws Exception {
        mockMvc.perform(patch(RUTA + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en_camino\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@duoc.cl", roles = "ADMIN")
    void cambiarEstadoInvalido400() throws Exception {
        mockMvc.perform(patch(RUTA + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"cancelado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@duoc.cl", roles = "ADMIN")
    void cambiarEstadoAdmin200() throws Exception {
        when(boletaService.cambiarEstado(eq(1L), any())).thenReturn(RESPUESTA);

        mockMvc.perform(patch(RUTA + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en_camino\"}"))
                .andExpect(status().isOk());

        verify(boletaService).cambiarEstado(eq(1L), any());
    }
}