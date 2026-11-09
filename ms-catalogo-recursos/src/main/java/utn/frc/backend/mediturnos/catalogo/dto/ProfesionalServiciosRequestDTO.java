package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalServiciosRequestDTO {

    @NotEmpty(message = "La lista de identificadores de servicios no puede estar vacía")
    private List<Long> serviciosIds;
}
