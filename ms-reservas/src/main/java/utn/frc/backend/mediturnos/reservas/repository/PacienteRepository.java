package utn.frc.backend.mediturnos.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByKeycloakSub(String keycloakSub);

    Optional<Paciente> findByEmail(String email);

    Optional<Paciente> findByDni(String dni);

    boolean existsByKeycloakSub(String keycloakSub);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
