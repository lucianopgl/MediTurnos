package utn.frc.backend.mediturnos.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utn.frc.backend.mediturnos.catalogo.entity.Equipamiento;

import java.util.Collection;
import java.util.List;

@Repository
public interface EquipamientoRepository extends JpaRepository<Equipamiento, Long> {

    List<Equipamiento> findBySedeId(Long sedeId);

    List<Equipamiento> findBySedeIdAndOperativo(Long sedeId, Boolean operativo);

    List<Equipamiento> findByOperativo(Boolean operativo);

    boolean existsByCodigoInventarioIgnoreCase(String codigoInventario);

    List<Equipamiento> findAllByIdIn(Collection<Long> ids);
}
