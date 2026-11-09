package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.frc.backend.mediturnos.catalogo.entity.Especialidad;

import java.util.List;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Especialidad> findByActiva(Boolean activa);
}
