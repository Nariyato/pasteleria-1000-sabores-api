package cl.duoc.pasteleria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pasteleria.dto.CategoriaRequest;
import cl.duoc.pasteleria.dto.CategoriaResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.exception.ReglaNegocioException;
import cl.duoc.pasteleria.model.Categoria;
import cl.duoc.pasteleria.repository.CategoriaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria(Long id, String nombre) {
        return Categoria.builder().id(id).nombre(nombre).descripcion("Descripción").imagen("/img/a.jpg").build();
    }

    @Test
    void listarDevuelveTodasLasCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(1L, "Tortas"), categoria(2L, "Postres")));

        List<CategoriaResponse> resultado = categoriaService.listar();

        assertThat(resultado).extracting(CategoriaResponse::nombre).containsExactly("Tortas", "Postres");
    }

    @Test
    void obtenerPorIdDevuelveLaCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Tortas")));

        assertThat(categoriaService.obtenerPorId(1L).nombre()).isEqualTo("Tortas");
    }

    @Test
    void obtenerPorIdLanzaNoEncontradoSiNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearGuardaLaCategoriaSinEspaciosSobrantes() {
        when(categoriaRepository.existsByNombreIgnoreCase("Tortas")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria guardada = inv.getArgument(0);
            guardada.setId(10L);
            return guardada;
        });

        CategoriaResponse creada = categoriaService.crear(new CategoriaRequest("  Tortas ", "Desc", null));

        assertThat(creada.id()).isEqualTo(10L);
        assertThat(creada.nombre()).isEqualTo("Tortas");
    }

    @Test
    void crearLanzaConflictoSiElNombreYaExiste() {
        when(categoriaRepository.existsByNombreIgnoreCase("Tortas")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequest("Tortas", null, null)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizarCambiaLosDatos() {
        Categoria existente = categoria(1L, "Tortas");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Pasteles", 1L)).thenReturn(false);
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponse actualizada = categoriaService.actualizar(1L, new CategoriaRequest("Pasteles", "Nueva", "/img/b.jpg"));

        assertThat(actualizada.nombre()).isEqualTo("Pasteles");
        assertThat(actualizada.descripcion()).isEqualTo("Nueva");
    }

    @Test
    void actualizarLanzaConflictoSiOtraCategoriaTieneEseNombre() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Tortas")));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Postres", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.actualizar(1L, new CategoriaRequest("Postres", null, null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void eliminarBorraLaCategoriaSinProductos() {
        Categoria existente = categoria(1L, "Tortas");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(false);

        categoriaService.eliminar(1L);

        verify(categoriaRepository).delete(existente);
    }

    @Test
    void eliminarLanzaConflictoSiTieneProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Tortas")));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("productos");
        verify(categoriaRepository, never()).delete(any());
    }
}
