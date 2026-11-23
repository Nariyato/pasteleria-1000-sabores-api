package cl.duoc.pasteleria.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.dto.BlogRequest;
import cl.duoc.pasteleria.dto.BlogResponse;
import cl.duoc.pasteleria.exception.RecursoNoEncontradoException;
import cl.duoc.pasteleria.model.BlogPost;
import cl.duoc.pasteleria.repository.BlogPostRepository;

/** Lógica de negocio del blog. El controller solo recibe y responde, las reglas viven aquí. */
@Service
@Transactional(readOnly = true)
public class BlogService {

    private final BlogPostRepository blogPostRepository;

    public BlogService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    /** Las entradas más nuevas primero, como las muestra el front. */
    public List<BlogResponse> listar() {
        return blogPostRepository.findAllByOrderByFechaDesc().stream().map(BlogResponse::desde).toList();
    }

    public BlogResponse obtener(Long id) {
        return BlogResponse.desde(buscar(id));
    }

    @Transactional
    public BlogResponse crear(BlogRequest datos) {
        BlogPost post = BlogPost.builder()
                .titulo(datos.titulo().trim())
                .categoria(datos.categoria().trim())
                .fecha(datos.fecha())
                .autor(datos.autor())
                .imagen(datos.imagen())
                .resumen(datos.resumen())
                .contenido(new ArrayList<>(datos.contenido()))
                .build();
        return BlogResponse.desde(blogPostRepository.save(post));
    }

    @Transactional
    public BlogResponse actualizar(Long id, BlogRequest datos) {
        BlogPost post = buscar(id);
        post.setTitulo(datos.titulo().trim());
        post.setCategoria(datos.categoria().trim());
        post.setFecha(datos.fecha());
        post.setAutor(datos.autor());
        post.setImagen(datos.imagen());
        post.setResumen(datos.resumen());
        post.setContenido(new ArrayList<>(datos.contenido()));
        return BlogResponse.desde(blogPostRepository.save(post));
    }

    @Transactional
    public void eliminar(Long id) {
        blogPostRepository.delete(buscar(id));
    }

    private BlogPost buscar(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la entrada del blog con id " + id));
    }
}
