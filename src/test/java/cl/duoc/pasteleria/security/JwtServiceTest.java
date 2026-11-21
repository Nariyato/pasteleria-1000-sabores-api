package cl.duoc.pasteleria.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Usuario;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-pasteleria-1000-sabores-minimo-32-chars";

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        jwtService = new JwtService(SECRETO, 30, 7);
        usuario = Usuario.builder()
                .email("admin@duoc.cl")
                .rol(Rol.ADMIN)
                .build();
    }

    @Test
    void generaAccessYRefreshDistintos() {
        String access = jwtService.generarAccessToken(usuario);
        String refresh = jwtService.generarRefreshToken(usuario);
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();
        assertThat(access).isNotEqualTo(refresh);
    }

    @Test
    void emailSiValidoConAccessDevuelveCorreo() {
        String token = jwtService.generarAccessToken(usuario);
        assertThat(jwtService.emailSiValido(token, JwtService.TIPO_ACCESS)).isEqualTo("admin@duoc.cl");
    }

    @Test
    void tipoEquivocadoDevuelveNull() {
        String refresh = jwtService.generarRefreshToken(usuario);
        assertThat(jwtService.emailSiValido(refresh, JwtService.TIPO_ACCESS)).isNull();
    }

    @Test
    void tokenManipuladoDevuelveNull() {
        String token = jwtService.generarAccessToken(usuario);
        String[] partes = token.split("\\.");
        partes[2] = partes[2].substring(0, partes[2].length() - 5) + "XXXXX";
        assertThat(jwtService.emailSiValido(String.join(".", partes), JwtService.TIPO_ACCESS)).isNull();
    }

    @Test
    void tokenVencidoDevuelveNull() {
        JwtService corto = new JwtService(SECRETO, 0, 7);
        String token = corto.generarAccessToken(usuario);
        assertThat(corto.emailSiValido(token, JwtService.TIPO_ACCESS)).isNull();
    }

    @Test
    void claimRolCorrecto() {
        String token = jwtService.generarAccessToken(usuario);
        assertThat(jwtService.leer(token).get("rol", String.class)).isEqualTo("ADMIN");
    }
}