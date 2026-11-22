package cl.duoc.pasteleria;

import static org.hamcrest.Matchers.notNullValue;
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
import org.springframework.test.web.servlet.MvcResult;

/** Mensajes de punta a punta con H2 y tokens reales obtenidos de /auth/login (sin simular usuarios). */
@SpringBootTest
@AutoConfigureMockMvc
class MensajeIntegracionTest {

    private static final String RUTA = "/api/v1/mensajes";
    private static final String JSON =
            "{\"tipo\":\"Sugerencia\",\"nombre\":\"Visitante\",\"email\":\"visita@correo.cl\",\"mensaje\":\"Hola\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonimoPuedeEnviarUnMensajeSinUsuarioAsociado() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    void clienteConTokenEnviaYElMensajeQuedaAsociadoASuUsuario() throws Exception {
        String access = loginAccess("rosa@gmail.com", "1234");
        String idRosa = extraerCampo(mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(post(RUTA).header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(Long.parseLong(idRosa)));
    }

    @Test
    void tokenBasuraEnElPostPublicoIgualPermiteEnviarSinUsuario() throws Exception {
        mockMvc.perform(post(RUTA).header("Authorization", "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    void anonimoNoPuedeListarLosMensajes() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeListarLosMensajes() throws Exception {
        String access = loginAccess("rosa@gmail.com", "1234");
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminVeElMensajeEnviadoLoAbreYLoElimina() throws Exception {
        String admin = loginAccess("admin@duoc.cl", "admin123");
        String id = extraerCampo(mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
        mockMvc.perform(get(RUTA + "/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Sugerencia"));
        mockMvc.perform(delete(RUTA + "/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(RUTA + "/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    private String loginAccess(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return extraerCampo(login.getResponse().getContentAsString(), "accessToken");
    }

    /** Saca el valor de un campo del JSON (texto o número) sin depender de una librería extra. */
    private static String extraerCampo(String json, String campo) {
        return json.replaceAll(".*?\"" + campo + "\":\"?([^\",}]+)\"?.*", "$1");
    }
}
