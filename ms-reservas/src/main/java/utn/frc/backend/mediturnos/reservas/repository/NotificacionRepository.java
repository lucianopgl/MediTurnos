package utn.frc.backend.mediturnos.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.reservas.entity.Notificacion;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findAllByOrderByFechaEnvioDesc();

    List<Notificacion> findByPacienteIdOrderByFechaEnvioDesc(Long pacienteId);

    List<Notificacion> findByEstadoEnvioOrderByFechaEnvioDesc(EstadoEnvioNotificacion estadoEnvio);

    List<Notificacion> findByTurnoIdOrderByFechaEnvioDesc(Long turnoId);
}
