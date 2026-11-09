package utn.frc.backend.mediturnos.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeResponseDTO {

    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;
    private Boolean activa;
}
