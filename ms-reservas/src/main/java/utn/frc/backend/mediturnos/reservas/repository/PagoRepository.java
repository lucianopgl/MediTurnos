package utn.frc.backend.mediturnos.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.frc.backend.mediturnos.reservas.entity.Pago;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByComprobanteReferencia(String comprobanteReferencia);

    boolean existsByComprobanteReferencia(String comprobanteReferencia);

    List<Pago> findByTurnoId(Long turnoId);
}
