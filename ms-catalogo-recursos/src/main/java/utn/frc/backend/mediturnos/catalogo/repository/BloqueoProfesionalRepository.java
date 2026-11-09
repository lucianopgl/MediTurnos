package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.BloqueoProfesional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BloqueoProfesionalRepository extends JpaRepository<BloqueoProfesional, Long> {

    List<BloqueoProfesional> findByProfesionalId(Long profesionalId);

    List<BloqueoProfesional> findByProfesionalIdAndFecha(Long profesionalId, LocalDate fecha);

    @Query("SELECT b FROM BloqueoProfesional b " +
           "WHERE b.profesional.id = :profesionalId " +
           "AND b.fecha = :fecha " +
           "AND b.horaInicio < :horaFin " +
           "AND b.horaFin > :horaInicio")
    List<BloqueoProfesional> findSolapamientos(@Param("profesionalId") Long profesionalId,
                                               @Param("fecha") LocalDate fecha,
                                               @Param("horaInicio") LocalTime horaInicio,
                                               @Param("horaFin") LocalTime horaFin);
}
