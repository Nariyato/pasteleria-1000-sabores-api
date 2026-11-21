package cl.duoc.pasteleria.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import cl.duoc.pasteleria.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** 403: el usuario está autenticado pero su rol no tiene permiso para esta ruta. */
@Component
public class RespuestaAccesoDenegado implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespuestaAccesoDenegado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorRespuesta(LocalDateTime.now(), 403, "Forbidden",
                "No tienes permiso para realizar esta acción", request.getRequestURI(), null));
    }
}