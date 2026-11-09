package utn.frc.backend.mediturnos.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultorioResponseDTO {

    private Long id;
    private Long sedeId;
    private String sedeNombre;
    private String codigoNombre;
    private Boolean operativo;
}
