package cl.duoc.pasteleria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pasteleria.model.DetalleBoleta;

@Repository
public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    List<DetalleBoleta> findByBoletaId(Long boletaId);
}
