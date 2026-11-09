package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultorioRequestDTO {

    @NotNull(message = "El identificador de la sede es obligatorio")
    private Long sedeId;

    @NotBlank(message = "El código o nombre del consultorio es obligatorio")
    private String codigoNombre;

    @Builder.Default
    private Boolean operativo = true;
}
