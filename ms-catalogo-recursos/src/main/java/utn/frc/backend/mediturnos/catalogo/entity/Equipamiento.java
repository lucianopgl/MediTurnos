package utn.frc.backend.mediturnos.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipamientos", schema = "schema_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "codigo_inventario", nullable = false, unique = true, length = 80)
    private String codigoInventario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean operativo = true;
}
