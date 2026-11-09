package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sedes", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(length = 50)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
