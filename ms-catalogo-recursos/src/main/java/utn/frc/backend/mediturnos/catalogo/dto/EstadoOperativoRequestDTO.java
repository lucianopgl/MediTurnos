package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoOperativoRequestDTO {

    @NotNull(message = "El estado operativo (true/false) es obligatorio")
    private Boolean operativo;
}
