package cl.duoc.pasteleria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

/** Prueba de punta a punta con H2: arranca toda la app, carga las semillas y llama a la API de verdad. */
@SpringBootTest
@AutoConfigureMockMvc
class CategoriaIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BoletaRepository boletaRepository;

    @Test
    void lasSemillasSeCargaronAlArrancar() {
        assertThat(productoRepository.count()).isEqualTo(16);
        assertThat(boletaRepository.count()).isEqualTo(2);
        assertThat(usuarioRepository.findByEmailIgnoreCase("VENDEDOR@pasteleria.cl")).isPresent();
        assertThat(usuarioRepository.findByEmailIgnoreCase("admin@duoc.cl").orElseThrow().getPassword())
                .hasSize(60)
                .startsWith("$2");
    }

    @Test
    void listarDevuelveLasOchoCategoriasSemilla() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].nombre").value("Tortas Cuadradas"));
    }

    @Test
    void noSePuedeEliminarUnaCategoriaConProductos() throws Exception {
        mockMvc.perform(delete("/api/v1/categorias/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    void crearYEliminarUnaCategoriaNueva() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Categoría de prueba\",\"descripcion\":\"Solo para el test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = respuesta.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(delete("/api/v1/categorias/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/categorias/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unaRutaInexistenteResponde404ConElFormatoDeError() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ruta").value("/api/v1/no-existe"));
    }
}
