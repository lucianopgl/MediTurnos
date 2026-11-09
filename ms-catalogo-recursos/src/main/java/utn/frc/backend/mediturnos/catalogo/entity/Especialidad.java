package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especialidades", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
