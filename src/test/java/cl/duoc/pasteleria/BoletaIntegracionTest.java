package cl.duoc.pasteleria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class BoletaIntegracionTest {

    private static final String JSON_COMPRA =
            "{\"items\":[{\"codigo\":\"TC001\",\"cantidad\":1,\"tamano\":\"chica\"},"
                    + "{\"codigo\":\"PT001\",\"cantidad\":2}],"
                    + "\"despacho\":{\"nombre\":\"Camila Pérez\",\"calle\":\"Av. Central 100\","
                    + "\"comuna\":\"Santiago\",\"fechaEntrega\":\"2026-12-01\"}}";

    @Autowired
    private MockMvc mockMvc;

  private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void compraCamilaMisBoletasYStock() throws Exception {
        String camila = loginAccess("camila@duocuc.cl", "1234");

        MvcResult producto = mockMvc.perform(get("/api/v1/productos/PT001"))
                .andExpect(status().isOk())
                .andReturn();
        int stockAntes = mapper.readTree(producto.getResponse().getContentAsString()).get("stock").asInt();

        MvcResult creada = mockMvc.perform(post("/api/v1/boletas")
                        .header("Authorization", "Bearer " + camila)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPRA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("preparacion"))
                .andExpect(jsonPath("$.costoDespacho").value(3500))
                .andReturn();

        JsonNode boleta = mapper.readTree(creada.getResponse().getContentAsString());
        assertThat(boleta.get("descuento").asInt()).isGreaterThan(0);
        long boletaId = boleta.get("id").asLong();

        mockMvc.perform(get("/api/v1/boletas/mis-boletas").header("Authorization", "Bearer " + camila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(boletaId));

        MvcResult productoDespues = mockMvc.perform(get("/api/v1/productos/PT001"))
                .andExpect(status().isOk())
                .andReturn();
        int stockDespues = mapper.readTree(productoDespues.getResponse().getContentAsString()).get("stock").asInt();
        assertThat(stockDespues).isEqualTo(stockAntes - 2);

        String vendedor = loginAccess("vendedor@pasteleria.cl", "vendedor123");
        mockMvc.perform(get("/api/v1/boletas").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/boletas/" + boletaId + "/estado")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en_camino\"}"))
                .andExpect(status().isForbidden());

        String admin = loginAccess("admin@duoc.cl", "admin123");
        mockMvc.perform(patch("/api/v1/boletas/" + boletaId + "/estado")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en_camino\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("en_camino"));

        String rosa = loginAccess("rosa@gmail.com", "1234");
        mockMvc.perform(get("/api/v1/boletas/" + boletaId).header("Authorization", "Bearer " + rosa))
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