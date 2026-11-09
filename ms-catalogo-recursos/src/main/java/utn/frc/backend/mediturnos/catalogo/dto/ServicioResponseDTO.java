package utn.frc.backend.mediturnos.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResponseDTO {

    private Long id;
    private Long especialidadId;
    private String especialidadNombre;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private BigDecimal costo;
    private Boolean activo;
    private List<EquipamientoResponseDTO> equipamientosRequeridos;
}
