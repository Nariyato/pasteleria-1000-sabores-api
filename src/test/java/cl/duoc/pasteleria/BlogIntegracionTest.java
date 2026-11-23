package cl.duoc.pasteleria;

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

/** Blog de punta a punta con H2: arranca toda la app, carga las semillas y llama a la API de verdad. */
@SpringBootTest
@AutoConfigureMockMvc
class BlogIntegracionTest {

    private static final String RUTA = "/api/v1/blog";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cualquierPersonaVeLasCuatroEntradasSemillaDeLaMasNuevaALaMasAntigua() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].titulo").value("5 Errores Comunes al Batir Merengue"))
                .andExpect(jsonPath("$[0].contenido.length()").value(3));
    }

    @Test
    void crearUnaEntradaSinTokenResponde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sin sesión\",\"categoria\":\"Recetas\",\"fecha\":\"2026-11-18\","
                                + "\"contenido\":[\"Un párrafo\"]}"))
                .andExpect(status().isUnauthorized());
    }
}
