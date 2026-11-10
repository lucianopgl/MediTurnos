package utn.frc.backend.mediturnos.reservas.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.reservas.entity.Turno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TurnoSpecifications {

    public static Specification<Turno> conFiltros(Long pacienteId, Long profesionalId, Long sedeId,
                                                  LocalDate fecha, EstadoTurno estado) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (pacienteId != null) {
                predicates.add(cb.equal(root.get("paciente").get("id"), pacienteId));
            }
            if (profesionalId != null) {
                predicates.add(cb.equal(root.get("profesionalId"), profesionalId));
            }
            if (sedeId != null) {
                predicates.add(cb.equal(root.get("sedeId"), sedeId));
            }
            if (fecha != null) {
                predicates.add(cb.equal(root.get("fecha"), fecha));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
