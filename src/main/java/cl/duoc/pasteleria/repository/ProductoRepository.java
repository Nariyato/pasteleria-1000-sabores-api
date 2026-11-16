package cl.duoc.pasteleria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pasteleria.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByCategoriaId(Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByDestacadoTrue();

    List<Producto> findByOfertaGreaterThan(int oferta);

    List<Producto> findByStockLessThan(int stock);
}
