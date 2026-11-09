package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.Consultorio;

import java.util.List;

@Repository
public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {

    List<Consultorio> findBySedeId(Long sedeId);

    List<Consultorio> findBySedeIdAndOperativo(Long sedeId, Boolean operativo);

    List<Consultorio> findByOperativo(Boolean operativo);

    boolean existsBySedeIdAndCodigoNombreIgnoreCase(Long sedeId, String codigoNombre);
}
