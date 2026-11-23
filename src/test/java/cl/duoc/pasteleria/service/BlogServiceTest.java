package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pasteleria.dto.BlogRequest;
import cl.duoc.pasteleria.dto.BlogResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.model.BlogPost;
import cl.duoc.pasteleria.repository.BlogPostRepository;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    private static final BlogRequest DATOS = new BlogRequest(" Merengue perfecto ", " Consejos de maestros ",
            LocalDate.parse("2026-11-18"), "Freddy Camposo", "/img/amasando.jpg", "Resumen corto",
            List.of("Primer párrafo", "Segundo párrafo"));

    @Mock
    private BlogPostRepository blogPostRepository;

    @InjectMocks
    private BlogService blogService;

    /** El repositorio simulado devuelve lo mismo que recibe, con id 1 (como haría la base). */
    private void guardarDevuelveConId() {
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocacion -> {
            BlogPost p = invocacion.getArgument(0);
            p.setId(1L);
            return p;
        });
    }

    private BlogPost post(Long id, String titulo) {
        return BlogPost.builder().id(id).titulo(titulo).categoria("Recetas tradicionales")
                .fecha(LocalDate.parse("2026-10-05")).autor("Ana").imagen("/img/manjar casero.jpg")
                .resumen("Resumen").contenido(new ArrayList<>(List.of("Párrafo uno", "Párrafo dos"))).build();
    }

    @Test
    void listarDevuelveLasEntradasEnElOrdenDelRepositorio() {
        when(blogPostRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(post(2L, "Nueva"), post(1L, "Antigua")));

        List<BlogResponse> lista = blogService.listar();

        assertThat(lista).extracting(BlogResponse::id).containsExactly(2L, 1L);
        assertThat(lista).extracting(BlogResponse::titulo).containsExactly("Nueva", "Antigua");
    }

    @Test
    void listarDevuelveListaVaciaSiNoHayEntradas() {
        when(blogPostRepository.findAllByOrderByFechaDesc()).thenReturn(List.of());

        assertThat(blogService.listar()).isEmpty();
    }

    @Test
    void obtenerDevuelveLaEntradaConSusParrafosEnOrden() {
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post(1L, "Manjar casero")));

        BlogResponse respuesta = blogService.obtener(1L);

        assertThat(respuesta.titulo()).isEqualTo("Manjar casero");
        assertThat(respuesta.contenido()).containsExactly("Párrafo uno", "Párrafo dos");
    }

    @Test
    void obtenerLanzaNoEncontradoSiNoExiste() {
        when(blogPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la entrada del blog con id 99");
    }

    @Test
    void crearGuardaLaEntradaYDevuelveSuId() {
        guardarDevuelveConId();

        BlogResponse creada = blogService.crear(DATOS);

        assertThat(creada.id()).isEqualTo(1L);
        assertThat(creada.autor()).isEqualTo("Freddy Camposo");
        assertThat(creada.fecha()).isEqualTo(LocalDate.parse("2026-11-18"));
        assertThat(creada.contenido()).containsExactly("Primer párrafo", "Segundo párrafo");
    }

    @Test
    void crearLimpiaLosEspaciosDelTituloYLaCategoria() {
        guardarDevuelveConId();
        ArgumentCaptor<BlogPost> guardado = ArgumentCaptor.forClass(BlogPost.class);

        blogService.crear(DATOS);

        verify(blogPostRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTitulo()).isEqualTo("Merengue perfecto");
        assertThat(guardado.getValue().getCategoria()).isEqualTo("Consejos de maestros");
    }

    @Test
    void actualizarCambiaTodosLosCamposDeLaEntrada() {
        BlogPost existente = post(1L, "Título viejo");
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(blogPostRepository.save(existente)).thenReturn(existente);

        BlogResponse actualizada = blogService.actualizar(1L, DATOS);

        assertThat(actualizada.id()).isEqualTo(1L);
        assertThat(actualizada.titulo()).isEqualTo("Merengue perfecto");
        assertThat(actualizada.categoria()).isEqualTo("Consejos de maestros");
        assertThat(actualizada.contenido()).containsExactly("Primer párrafo", "Segundo párrafo");
    }

    @Test
    void actualizarLanzaNoEncontradoSiNoExisteYNoGuarda() {
        when(blogPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.actualizar(99L, DATOS)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void eliminarBorraLaEntrada() {
        BlogPost existente = post(1L, "Para borrar");
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(existente));

        blogService.eliminar(1L);

        verify(blogPostRepository).delete(existente);
    }

    @Test
    void eliminarLanzaNoEncontradoSiNoExisteYNoBorra() {
        when(blogPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(blogPostRepository, never()).delete(any());
    }
}
