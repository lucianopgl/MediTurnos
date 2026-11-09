package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadRequestDTO {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String nombre;

    private String descripcion;

    @Builder.Default
    private Boolean activa = true;
}
