package cl.duoc.pasteleria.dto;

import java.time.Instant;

import cl.duoc.pasteleria.model.Mensaje;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que la API devuelve de un mensaje de contacto. usuarioId es null si se envió sin iniciar sesión. */
@Schema(description = "Mensaje del formulario de contacto")
public record MensajeResponse(Long id, String tipo, String nombre, String email, String mensaje, Long usuarioId,
        Instant fecha) {

    /** Debe llamarse dentro de la transacción: el usuario es una relación LAZY. */
    public static MensajeResponse desde(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getTipo(), m.getNombre(), m.getEmail(), m.getMensaje(),
                m.getUsuario() == null ? null : m.getUsuario().getId(), m.getFecha());
    }
}
