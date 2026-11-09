package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.Servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    @Query("SELECT s FROM Servicio s WHERE (:especialidadId IS NULL OR s.especialidad.id = :especialidadId) " +
           "AND (:activo IS NULL OR s.activo = :activo)")
    List<Servicio> searchByFiltros(@Param("especialidadId") Long especialidadId, @Param("activo") Boolean activo);

    @EntityGraph(attributePaths = {"equipamientosRequeridos", "especialidad"})
    @Query("SELECT s FROM Servicio s WHERE s.id = :id")
    Optional<Servicio> findWithDetailsById(@Param("id") Long id);

    List<Servicio> findAllByIdIn(Collection<Long> ids);
}
