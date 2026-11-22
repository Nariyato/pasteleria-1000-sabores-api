package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pasteleria.dto.MensajeRequest;
import cl.duoc.pasteleria.dto.MensajeResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.model.Mensaje;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.MensajeRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T15:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneId.of("America/Santiago"));
    private static final MensajeRequest DATOS =
            new MensajeRequest(" Reclamo ", " Rosa Muñoz ", " Rosa@Gmail.com ", " Mi torta llegó tarde ");

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private MensajeService mensajeService;

    @BeforeEach
    void configurarReloj() {
        mensajeService = new MensajeService(mensajeRepository, usuarioRepository, RELOJ);
    }

    /** El repositorio simulado devuelve lo mismo que recibe, con id 1 (como haría la base). */
    private void guardarDevuelveConId() {
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(invocacion -> {
            Mensaje m = invocacion.getArgument(0);
            m.setId(1L);
            return m;
        });
    }

    private Mensaje mensaje(Long id, Usuario usuario) {
        return Mensaje.builder().id(id).tipo("Sugerencia").nombre("Pedro").email("pedro@gmail.com")
                .mensaje("Hola").usuario(usuario).fecha(AHORA).build();
    }

    @Test
    void crearSinSesionGuardaElMensajeSinUsuario() {
        guardarDevuelveConId();

        MensajeResponse creado = mensajeService.crear(DATOS, null);

        assertThat(creado.usuarioId()).isNull();
        assertThat(creado.id()).isEqualTo(1L);
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void crearConSesionAsociaElUsuarioDelToken() {
        guardarDevuelveConId();
        Usuario rosa = Usuario.builder().id(3L).email("rosa@gmail.com").build();
        when(usuarioRepository.findByEmailIgnoreCase("rosa@gmail.com")).thenReturn(Optional.of(rosa));

        MensajeResponse creado = mensajeService.crear(DATOS, "rosa@gmail.com");

        assertThat(creado.usuarioId()).isEqualTo(3L);
    }

    @Test
    void crearLimpiaEspaciosYGuardaElCorreoEnMinusculasConLaFechaDelReloj() {
        guardarDevuelveConId();
        ArgumentCaptor<Mensaje> guardado = ArgumentCaptor.forClass(Mensaje.class);

        mensajeService.crear(DATOS, null);

        verify(mensajeRepository).save(guardado.capture());
        Mensaje m = guardado.getValue();
        assertThat(m.getTipo()).isEqualTo("Reclamo");
        assertThat(m.getNombre()).isEqualTo("Rosa Muñoz");
        assertThat(m.getEmail()).isEqualTo("rosa@gmail.com");
        assertThat(m.getMensaje()).isEqualTo("Mi torta llegó tarde");
        assertThat(m.getFecha()).isEqualTo(AHORA);
    }

    @Test
    void crearConUsuarioDelTokenInexistenteLanzaNoEncontradoYNoGuarda() {
        when(usuarioRepository.findByEmailIgnoreCase("borrado@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.crear(DATOS, "borrado@gmail.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void listarDevuelveLosMensajesEnElOrdenDelRepositorio() {
        Usuario camila = Usuario.builder().id(5L).build();
        when(mensajeRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(mensaje(2L, camila), mensaje(1L, null)));

        List<MensajeResponse> lista = mensajeService.listar();

        assertThat(lista).extracting(MensajeResponse::id).containsExactly(2L, 1L);
        assertThat(lista).extracting(MensajeResponse::usuarioId).containsExactly(5L, null);
    }

    @Test
    void obtenerDevuelveElMensaje() {
        when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje(1L, null)));

        assertThat(mensajeService.obtener(1L).nombre()).isEqualTo("Pedro");
    }

    @Test
    void obtenerLanzaNoEncontradoSiNoExiste() {
        when(mensajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el mensaje con id 99");
    }

    @Test
    void eliminarBorraElMensaje() {
        Mensaje m = mensaje(1L, null);
        when(mensajeRepository.findById(1L)).thenReturn(Optional.of(m));

        mensajeService.eliminar(1L);

        verify(mensajeRepository).delete(m);
    }

    @Test
    void eliminarLanzaNoEncontradoSiNoExiste() {
        when(mensajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(mensajeRepository, never()).delete(any());
    }
}
