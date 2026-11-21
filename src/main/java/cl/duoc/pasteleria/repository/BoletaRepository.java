package cl.duoc.pasteleria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pasteleria.model.Boleta;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    List<Boleta> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);
}
