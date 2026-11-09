package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.HorarioProfesional;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;

import java.util.List;

@Repository
public interface HorarioProfesionalRepository extends JpaRepository<HorarioProfesional, Long> {

    List<HorarioProfesional> findByProfesionalIdAndActivoTrue(Long profesionalId);

    List<HorarioProfesional> findByProfesionalIdAndDiaSemanaAndActivoTrue(Long profesionalId, DiaSemana diaSemana);

    List<HorarioProfesional> findByProfesionalId(Long profesionalId);
}
