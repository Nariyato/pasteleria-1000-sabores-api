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

/** Pruebas de seguridad con tokens reales sobre H2. */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegracionTest {

    private static final String JSON_CATEGORIA = "{\"nombre\":\"Seguridad\",\"descripcion\":\"Test\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinTokenAlCrearCategoriaResponde401() throws Exception {
        mockMvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_CATEGORIA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void clienteNoPuedeCrearCategoria() throws Exception {
        String access = loginAccess("rosa@gmail.com", "1234");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CATEGORIA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void adminPuedeCrearCategoria() throws Exception {
        String access = loginAccess("admin@duoc.cl", "admin123");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CATEGORIA))
                .andExpect(status().isCreated());
    }

    @Test
    void listarCategoriasEsPublicoSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)));
    }

    @Test
    void tokenBasuraEnRutaPublicaSigueRespondiendo200() throws Exception {
        mockMvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenNoSirveComoAccess() throws Exception {
        String refresh = loginRefresh("admin@duoc.cl", "admin123");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + refresh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CATEGORIA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenNoSirveEnRefresh() throws Exception {
        String access = loginAccess("admin@duoc.cl", "admin123");
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El token de renovación es inválido o expiró"));
    }

    @Test
    void refreshValidoDevuelveTokensNuevos() throws Exception {
        String refresh = loginRefresh("admin@duoc.cl", "admin123");
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void vendedorNoPuedeCrearCategoria() throws Exception {
        String access = loginAccess("vendedor@pasteleria.cl", "vendedor1");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CATEGORIA))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeListarUsuarios() throws Exception {
        String access = loginAccess("rosa@gmail.com", "1234");
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
    }

    private String loginAccess(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return extraerCampo(login.getResponse().getContentAsString(), "accessToken");
    }

    private String loginRefresh(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return extraerCampo(login.getResponse().getContentAsString(), "refreshToken");
    }

    private static String extraerCampo(String json, String campo) {
        return json.replaceAll(".*\"" + campo + "\":\"([^\"]+)\".*", "$1");
    }
}