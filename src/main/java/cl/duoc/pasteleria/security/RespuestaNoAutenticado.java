package cl.duoc.pasteleria.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import cl.duoc.pasteleria.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** 401: no hay token, o es inválido o está vencido. Responde con el mismo JSON de error que el resto de la API. */
@Component
public class RespuestaNoAutenticado implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RespuestaNoAutenticado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorRespuesta(LocalDateTime.now(), 401, "Unauthorized",
                "Debes iniciar sesión o tu sesión expiró", request.getRequestURI(), null));
    }
}