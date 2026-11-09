package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.Sede;

import java.util.List;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Sede> findByActiva(Boolean activa);
}
