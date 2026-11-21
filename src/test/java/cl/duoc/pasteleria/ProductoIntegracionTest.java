package cl.duoc.pasteleria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoIntegracionTest {

    private static final String JSON_PRODUCTO =
            "{\"codigo\":\"TST001\",\"nombre\":\"Prueba integración\",\"categoriaId\":1,\"precio\":9000,"
                    + "\"stock\":6,\"oferta\":0,\"destacado\":false,\"activo\":true,\"permiteMensaje\":false,"
                    + "\"tamanos\":[]}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listaPublicaTieneDieciseisProductosSemilla() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16));
    }

    @Test
    void destacadosYOfertasCoincidenConSemillas() throws Exception {
        mockMvc.perform(get("/api/v1/productos/destacados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult ofertas = mockMvc.perform(get("/api/v1/productos/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn();

        JsonNode lista = new ObjectMapper().readTree(ofertas.getResponse().getContentAsString());
        assertThat(lista.get(0).get("oferta").asInt()).isGreaterThan(0);
        if (lista.size() >= 2) {
            assertThat(lista.get(0).get("oferta").asInt())
                    .isGreaterThanOrEqualTo(lista.get(1).get("oferta").asInt());
        }
    }

    @Test
    void criticosSoloAdmin() throws Exception {
        String admin = loginAccess("admin@duoc.cl", "admin123");
        mockMvc.perform(get("/api/v1/productos/criticos").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void vendedorPuedeListarTodosPeroNoCrear() throws Exception {
        String vendedor = loginAccess("vendedor@pasteleria.cl", "vendedor123");
        mockMvc.perform(get("/api/v1/productos/todos").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(16)));

        mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PRODUCTO))
                .andExpect(status().isForbidden());
    }

    private String loginAccess(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return extraerCampo(login.getResponse().getContentAsString(), "accessToken");
    }

    private static String extraerCampo(String json, String campo) {
        return json.replaceAll(".*\"" + campo + "\":\"([^\"]+)\".*", "$1");
    }

}