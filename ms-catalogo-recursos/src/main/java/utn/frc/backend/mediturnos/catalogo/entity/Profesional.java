package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profesionales", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_sub", nullable = false, unique = true, length = 64)
    private String keycloakSub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_fijo_id", nullable = false)
    private Consultorio consultorioFijo;

    @Column(nullable = false, unique = true, length = 50)
    private String matricula;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "profesional_servicios",
            schema = "schema_catalogo",
            joinColumns = @JoinColumn(name = "profesional_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    @Builder.Default
    private Set<Servicio> serviciosHabilitados = new HashSet<>();
}
