package cl.duoc.pasteleria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pasteleria.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByActivoTrue();

    List<Producto> findByActivoTrueAndDestacadoTrue();

    List<Producto> findByActivoTrueAndOfertaGreaterThanOrderByOfertaDesc(int oferta);

    List<Producto> findByStockLessThan(int stock);
}
