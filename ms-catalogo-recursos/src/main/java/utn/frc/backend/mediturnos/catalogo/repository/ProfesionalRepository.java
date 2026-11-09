package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utn.frc.backend.mediturnos.catalogo.entity.Profesional;

import java.util.List;
import java.util.Optional;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {

    boolean existsByKeycloakSub(String keycloakSub);

    boolean existsByMatriculaIgnoreCase(String matricula);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Profesional> findByKeycloakSub(String keycloakSub);

    @Query("SELECT DISTINCT p FROM Profesional p " +
           "LEFT JOIN p.serviciosHabilitados s " +
           "WHERE (:especialidadId IS NULL OR p.especialidad.id = :especialidadId) " +
           "AND (:servicioId IS NULL OR s.id = :servicioId) " +
           "AND (:activo IS NULL OR p.activo = :activo)")
    List<Profesional> searchByFiltros(@Param("especialidadId") Long especialidadId,
                                     @Param("servicioId") Long servicioId,
                                     @Param("activo") Boolean activo);

    @EntityGraph(attributePaths = {"serviciosHabilitados", "consultorioFijo", "consultorioFijo.sede", "especialidad"})
    @Query("SELECT p FROM Profesional p WHERE p.id = :id")
    Optional<Profesional> findWithDetailsById(@Param("id") Long id);
}
