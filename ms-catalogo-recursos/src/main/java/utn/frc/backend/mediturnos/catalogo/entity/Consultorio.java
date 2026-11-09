package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consultorios", schema = "schema_catalogo",
        uniqueConstraints = @UniqueConstraint(name = "uk_consultorio_sede_codigo", columnNames = {"sede_id", "codigo_nombre"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "codigo_nombre", nullable = false, length = 80)
    private String codigoNombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean operativo = true;
}
