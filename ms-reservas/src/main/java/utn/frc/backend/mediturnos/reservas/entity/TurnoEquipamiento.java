package utn.frc.backend.mediturnos.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "turno_equipamientos", schema = "schema_reservas",
       uniqueConstraints = @UniqueConstraint(name = "uk_turno_equipamiento", columnNames = {"turno_id", "equipamiento_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Column(name = "equipamiento_id", nullable = false)
    private Long equipamientoId;
}
