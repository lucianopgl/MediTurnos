package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_profesional", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 20)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
