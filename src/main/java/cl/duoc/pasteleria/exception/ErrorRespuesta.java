package cl.duoc.pasteleria.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Formato único de todos los errores de la API. "errores" solo aparece en las validaciones (400). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> errores) {
}
