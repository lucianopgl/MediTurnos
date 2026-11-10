package utn.frc.backend.mediturnos.reservas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turnos", schema = "schema_reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "consultorio_id", nullable = false)
    private Long consultorioId;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "costo_acordado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoAcordado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoTurno estado;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "turno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TurnoEquipamiento> equipamientos = new ArrayList<>();

    public void agregarEquipamiento(Long equipamientoId) {
        TurnoEquipamiento te = TurnoEquipamiento.builder()
                .turno(this)
                .equipamientoId(equipamientoId)
                .build();
        this.equipamientos.add(te);
    }
}
