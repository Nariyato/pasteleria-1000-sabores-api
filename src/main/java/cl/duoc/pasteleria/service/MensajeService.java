package cl.duoc.pasteleria.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.MensajeRequest;
import cl.duoc.pasteleria.dto.MensajeResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.model.Mensaje;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.MensajeRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

/** Mensajes del formulario de contacto. Se crean y se leen; no se editan (son un registro de lo que escribió el cliente). */
@Service
@Transactional(readOnly = true)
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    public MensajeService(MensajeRepository mensajeRepository, UsuarioRepository usuarioRepository, Clock reloj) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
    }

    /** emailOpcional es el correo del token si hay sesión, o null si se escribió sin iniciar sesión. */
    @Transactional
    public MensajeResponse crear(MensajeRequest datos, String emailOpcional) {
        Usuario usuario = emailOpcional == null ? null : usuarioRepository.findByEmailIgnoreCase(emailOpcional)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + emailOpcional));
        Mensaje mensaje = Mensaje.builder()
                .tipo(datos.tipo().trim())
                .nombre(datos.nombre().trim())
                .email(datos.email().trim().toLowerCase())
                .mensaje(datos.mensaje().trim())
                .usuario(usuario)
                .fecha(Instant.now(reloj))
                .build();
        return MensajeResponse.desde(mensajeRepository.save(mensaje));
    }

    public List<MensajeResponse> listar() {
        return mensajeRepository.findAllByOrderByFechaDesc().stream().map(MensajeResponse::desde).toList();
    }

    public MensajeResponse obtener(Long id) {
        return MensajeResponse.desde(buscar(id));
    }

    @Transactional
    public void eliminar(Long id) {
        mensajeRepository.delete(buscar(id));
    }

    private Mensaje buscar(Long id) {
        return mensajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el mensaje con id " + id));
    }
}
