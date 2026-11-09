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
public class SedeRequestDTO {

    @NotBlank(message = "El nombre de la sede es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección de la sede es obligatoria")
    private String direccion;

    private String telefono;

    @Builder.Default
    private Boolean activa = true;
}
