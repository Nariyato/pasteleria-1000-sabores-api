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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clienteNoPuedeListarUsuarios() throws Exception {
        String access = loginAccess("rosa@gmail.com", "1234");
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void adminListaUsuariosSinExponerPassword() throws Exception {
        String access = loginAccess("admin@duoc.cl", "admin123");
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void vendedorNoPuedeCrearUsuario() throws Exception {
        String access = loginAccess("vendedor@pasteleria.cl", "vendedor123");
        String json =
                "{\"run\":\"999999990\",\"nombre\":\"Extra\",\"apellidos\":\"Test\",\"email\":\"extra@test.cl\","
                        + "\"fechaNacimiento\":\"1990-01-01\",\"password\":\"1234\",\"rol\":\"cliente\"}";
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
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