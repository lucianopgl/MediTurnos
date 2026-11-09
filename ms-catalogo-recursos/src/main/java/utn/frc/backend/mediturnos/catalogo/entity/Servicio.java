package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servicios", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "servicio_equipamientos",
            schema = "schema_catalogo",
            joinColumns = @JoinColumn(name = "servicio_id"),
            inverseJoinColumns = @JoinColumn(name = "equipamiento_id")
    )
    @Builder.Default
    private Set<Equipamiento> equipamientosRequeridos = new HashSet<>();
}
